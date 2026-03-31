package mods.eln.falstad

import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.addChatMessage
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.sixnode.CurrentSourceElement
import mods.eln.sixnode.CreativePowerCapacitorElement
import mods.eln.sixnode.CreativePowerInductorElement
import mods.eln.sixnode.CreativePowerResistorElement
import mods.eln.sixnode.PowerCapacitorSixContainer
import mods.eln.sixnode.PowerCapacitorSixElement
import mods.eln.sixnode.PowerInductorSixContainer
import mods.eln.sixnode.PowerInductorSixElement
import mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayElement
import mods.eln.sixnode.electricalsensor.ElectricalSensorElement
import mods.eln.sixnode.electricalsource.ElectricalSourceElement
import mods.eln.sixnode.electricalswitch.ElectricalSwitchElement
import mods.eln.sixnode.resistor.ResistorDescriptor
import mods.eln.sixnode.resistor.ResistorElement
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor

object FalstadImporter {
    private data class Area(val originX: Int, val groundY: Int, val originZ: Int)
    private data class VoltageSourcePlacement(val sourcePoint: FalstadPoint, val groundPoint: FalstadPoint, val voltage: Double)
    private data class SinglePortVoltageSourcePlacement(val sourcePoint: FalstadPoint, val voltage: Double)
    private data class ProbeDisplayPlacement(
        val probePoint: FalstadPoint,
        val displayPoint: FalstadPoint,
        val dotSourcePoint: FalstadPoint,
        val probeFront: LRDU,
        val displayFront: LRDU
    )
    private data class PlacementResult(val placed: Boolean, val failureSummary: String? = null)
    private val planningExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "eln-falstad-import").apply { isDaemon = true }
    }

    fun importFromClipboardAsync(player: EntityPlayerMP, netlist: String) {
        val playerName = player.commandSenderName
        val dimension = player.worldObj.provider.dimensionId
        planningExecutor.execute {
            val parseResult = try {
                FalstadDeviceParser.parse(netlist)
            } catch (_: Exception) {
                Eln.delayedTask.add(object : mods.eln.server.DelayedTaskManager.ITask {
                    override fun run() {
                        val livePlayer = player.mcServer.configurationManager.func_152612_a(playerName)
                        if (livePlayer != null) {
                            addChatMessage(livePlayer, "Falstad import: clipboard is not valid Falstad data.")
                        }
                    }
                })
                return@execute
            }
            val plan = FalstadLayoutPlanner.plan(parseResult)
            Eln.delayedTask.add(object : mods.eln.server.DelayedTaskManager.ITask {
                override fun run() {
                    val livePlayer = player.mcServer.configurationManager.func_152612_a(playerName)
                    if (livePlayer == null || livePlayer.worldObj.provider.dimensionId != dimension) {
                        return
                    }
                    importPlanned(livePlayer, plan)
                }
            })
        }
    }

    private fun importPlanned(player: EntityPlayerMP, plan: FalstadLayoutPlan) {
        if (plan.width <= 0 || plan.height <= 0) {
            addChatMessage(player, "Falstad import: no components found.")
            return
        }

        val area = findPlacementArea(player.worldObj, player, plan.width, plan.height)
        if (area == null) {
            addChatMessage(player, "Falstad import: couldn't find a flat, clear area near the player.")
            return
        }

        val messages = mutableListOf<String>()
        val probeDisplayMax = probeDisplayMaxValue(plan)
        val sourcedByVoltageSource = plan.components
            .mapNotNull {
                when (it.kind) {
                    FalstadPlacedKind.VOLTAGE_SOURCE -> voltageSourcePlacement(it).sourcePoint
                    FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE -> singlePortVoltageSourcePlacement(it).sourcePoint
                    else -> null
                }
            }
            .toSet()
        val groundedByVoltageSource = plan.components
            .filter { it.kind == FalstadPlacedKind.VOLTAGE_SOURCE }
            .map { voltageSourcePlacement(it).groundPoint }
            .toSet()

        for ((point, kind) in plan.nodes) {
            if (point in sourcedByVoltageSource) {
                continue
            }
            val descriptor = when (kind) {
                FalstadNodeKind.GROUND -> getBlockDescriptor("Ground Cable")
                FalstadNodeKind.NORMAL -> if (point in groundedByVoltageSource) getBlockDescriptor("Ground Cable") else Eln.instance.lowVoltageCableDescriptor
            } ?: continue

            val result = placeSixNode(player.worldObj, player, area, point, descriptor, null)
            if (!result.placed) {
                messages += "Failed to place ${descriptor.name} at ${point.x},${point.y}"
                result.failureSummary?.let { messages += "Debug ${descriptor.name} at ${point.x},${point.y}: $it" }
            }
        }

        for (point in plan.wires) {
            val result = placeSixNode(player.worldObj, player, area, point, Eln.instance.lowVoltageCableDescriptor, null)
            if (!result.placed) {
                messages += "Failed to place wire at ${point.x},${point.y}"
                result.failureSummary?.let { messages += "Debug wire at ${point.x},${point.y}: $it" }
            }
        }

        for (component in plan.components) {
            if (component.kind == FalstadPlacedKind.VOLTAGE_SOURCE) {
                val result = placeVoltageSource(player, area, component, messages)
                if (!result.placed) {
                    messages += "Failed to place Falstad voltage source at ${component.cell.x},${component.cell.y}"
                    result.failureSummary?.let { messages += "Debug Falstad voltage source at ${component.cell.x},${component.cell.y}: $it" }
                }
                messages += "Voltage source substituted with Electrical Source + Ground"
                continue
            }
            if (component.kind == FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE) {
                val result = placeSinglePortVoltageSource(player, area, component, messages)
                if (!result.placed) {
                    messages += "Failed to place Falstad voltage source at ${component.cell.x},${component.cell.y}"
                    result.failureSummary?.let { messages += "Debug Falstad voltage source at ${component.cell.x},${component.cell.y}: $it" }
                }
                messages += "Legacy adjustable voltage source substituted with Electrical Source"
                continue
            }
            if (component.kind == FalstadPlacedKind.PROBE_DISPLAY) {
                val result = placeProbeDisplay(player, area, component, probeDisplayMax, messages)
                if (!result.placed) {
                    messages += "Failed to place Falstad probe/display at ${component.cell.x},${component.cell.y}"
                    result.failureSummary?.let { messages += "Debug Falstad probe/display at ${component.cell.x},${component.cell.y}: $it" }
                }
                continue
            }

            val descriptor = descriptorFor(component.kind)
            if (descriptor == null) {
                messages += "Missing ELN descriptor for ${component.kind}"
                continue
            }
            val result = placeSixNode(player.worldObj, player, area, component.cell, descriptor, frontFor(component))
            if (!result.placed) {
                messages += "Failed to place ${descriptor.name} at ${component.cell.x},${component.cell.y}"
                result.failureSummary?.let { messages += "Debug ${descriptor.name} at ${component.cell.x},${component.cell.y}: $it" }
                continue
            }
            val element = getTopElement(player.worldObj, area, component.cell)
            if (element != null) {
                configureElement(player, element, component, messages)
            }
            messages += component.substitutions
        }

        val totalPlaced = plan.nodes.size + plan.wires.size + plan.components.size
        addChatMessage(player, "Falstad import: placed $totalPlaced blocks.")
        for (message in plan.warnings.distinct().plus(messages.distinct()).distinct().take(8)) {
            addChatMessage(player, "Falstad import: $message")
        }
        if (plan.warnings.distinct().plus(messages.distinct()).distinct().size > 8) {
            addChatMessage(player, "Falstad import: additional warnings omitted.")
        }
    }

    private fun voltageSourcePlacement(component: FalstadPlacedComponent): VoltageSourcePlacement {
        val waveform = component.source.params.getOrNull(1)?.toIntOrNull() ?: 0
        val maxVoltage = component.source.params.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val offset = component.source.params.getOrNull(4)?.toDoubleOrNull() ?: 0.0
        val rawVoltage = if (waveform == 0) maxVoltage + offset else maxVoltage
        return if (rawVoltage >= 0.0) {
            VoltageSourcePlacement(component.end, component.start, rawVoltage)
        } else {
            VoltageSourcePlacement(component.start, component.end, -rawVoltage)
        }
    }

    private fun singlePortVoltageSourcePlacement(component: FalstadPlacedComponent): SinglePortVoltageSourcePlacement {
        val waveform = component.source.params.getOrNull(1)?.toIntOrNull() ?: 0
        val maxVoltage = component.source.params.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val offset = component.source.params.getOrNull(4)?.toDoubleOrNull() ?: 0.0
        val rawVoltage = if (waveform == 0) maxVoltage + offset else maxVoltage
        return SinglePortVoltageSourcePlacement(component.start, rawVoltage)
    }

    private fun probeDisplayPlacement(component: FalstadPlacedComponent): ProbeDisplayPlacement {
        return when (component.axis) {
            FalstadAxis.HORIZONTAL -> {
                if (component.start.x <= component.end.x) {
                    ProbeDisplayPlacement(
                        probePoint = component.cell,
                        displayPoint = component.end,
                        dotSourcePoint = FalstadPoint(component.end.x + 1, component.end.y),
                        probeFront = LRDU.Up,
                        displayFront = LRDU.Up
                    )
                } else {
                    ProbeDisplayPlacement(
                        probePoint = component.cell,
                        displayPoint = component.end,
                        dotSourcePoint = FalstadPoint(component.end.x - 1, component.end.y),
                        probeFront = LRDU.Down,
                        displayFront = LRDU.Down
                    )
                }
            }
            FalstadAxis.VERTICAL -> {
                if (component.start.y <= component.end.y) {
                    ProbeDisplayPlacement(
                        probePoint = component.cell,
                        displayPoint = component.end,
                        dotSourcePoint = FalstadPoint(component.end.x, component.end.y + 1),
                        probeFront = LRDU.Right,
                        displayFront = LRDU.Right
                    )
                } else {
                    ProbeDisplayPlacement(
                        probePoint = component.cell,
                        displayPoint = component.end,
                        dotSourcePoint = FalstadPoint(component.end.x, component.end.y - 1),
                        probeFront = LRDU.Left,
                        displayFront = LRDU.Left
                    )
                }
            }
        }
    }

    private fun probeDisplayMaxValue(plan: FalstadLayoutPlan): Double {
        val maxSourceVoltage = plan.components.mapNotNull { component ->
            when (component.kind) {
                FalstadPlacedKind.VOLTAGE_SOURCE -> abs(voltageSourcePlacement(component).voltage)
                FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE -> abs(singlePortVoltageSourcePlacement(component).voltage)
                else -> null
            }
        }.maxOrNull()
        return maxOf(maxSourceVoltage ?: 0.0, 1.0)
    }

    private fun placeVoltageSource(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = getBlockDescriptor("Electrical Source") ?: return PlacementResult(false, "missing descriptor: Electrical Source")
        val placement = voltageSourcePlacement(component)
        val placed = placeSixNode(player.worldObj, player, area, placement.sourcePoint, descriptor, frontFor(component))
        if (!placed.placed) return placed

        val element = getTopElement(player.worldObj, area, placement.sourcePoint) as? ElectricalSourceElement
        if (element == null) {
            messages += "Voltage source placed, but couldn't configure its voltage."
            return PlacementResult(true)
        }

        val compound = NBTTagCompound()
        compound.setDouble("voltage", placement.voltage)
        element.readConfigTool(compound, player)

        val waveform = component.source.params.getOrNull(1)?.toIntOrNull() ?: 0
        val frequency = component.source.params.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        if (waveform != 0 || abs(frequency) > 0.000001) {
            messages += "Line ${component.source.lineNumber}: non-DC source imported as DC."
        }
        return PlacementResult(true)
    }

    private fun placeSinglePortVoltageSource(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = getBlockDescriptor("Electrical Source") ?: return PlacementResult(false, "missing descriptor: Electrical Source")
        val placement = singlePortVoltageSourcePlacement(component)
        val placed = placeSixNode(player.worldObj, player, area, placement.sourcePoint, descriptor, frontFor(component))
        if (!placed.placed) return placed

        val element = getTopElement(player.worldObj, area, placement.sourcePoint) as? ElectricalSourceElement
        if (element == null) {
            messages += "Voltage source placed, but couldn't configure its voltage."
            return PlacementResult(true)
        }

        val compound = NBTTagCompound()
        compound.setDouble("voltage", placement.voltage)
        element.readConfigTool(compound, player)
        return PlacementResult(true)
    }

    private fun placeProbeDisplay(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        maxValue: Double,
        messages: MutableList<String>
    ): PlacementResult {
        val probeDescriptor = getBlockDescriptor("Voltage Probe") ?: return PlacementResult(false, "missing descriptor: Voltage Probe")
        val displayDescriptor = getBlockDescriptor("Digital Display") ?: return PlacementResult(false, "missing descriptor: Digital Display")
        val signalSourceDescriptor = getBlockDescriptor("Signal Source") ?: return PlacementResult(false, "missing descriptor: Signal Source")
        val placement = probeDisplayPlacement(component)

        val probeResult = placeSixNode(player.worldObj, player, area, placement.probePoint, probeDescriptor, placement.probeFront)
        if (!probeResult.placed) return probeResult
        val displayResult = placeSixNode(player.worldObj, player, area, placement.displayPoint, displayDescriptor, placement.displayFront)
        if (!displayResult.placed) return displayResult
        val dotSourceResult = placeSixNode(player.worldObj, player, area, placement.dotSourcePoint, signalSourceDescriptor, null)
        if (!dotSourceResult.placed) return dotSourceResult

        val decimalPlaces = component.source.params.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 3) ?: 0
        val displayMultiplier = (0 until decimalPlaces).fold(1.0) { acc, _ -> acc * 10.0 }
        val dotMask = decimalDotMask(decimalPlaces)

        val probeElement = getTopElement(player.worldObj, area, placement.probePoint) as? ElectricalSensorElement
        if (probeElement == null) {
            return PlacementResult(false, "placed Voltage Probe but couldn't find top element")
        }
        probeElement.inventory?.setInventorySlotContents(0, Eln.instance.lowVoltageCableDescriptor.newItemStack(1))
        probeElement.inventory?.markDirty()
        val probeConfig = NBTTagCompound()
        probeConfig.setFloat("min", 0.0f)
        probeConfig.setFloat("max", maxValue.toFloat())
        probeElement.readConfigTool(probeConfig, player)

        val displayElement = getTopElement(player.worldObj, area, placement.displayPoint) as? ElectricalDigitalDisplayElement
        if (displayElement == null) {
            return PlacementResult(false, "placed Digital Display but couldn't find top element")
        }
        val displayConfig = NBTTagCompound()
        displayConfig.setFloat("min", 0.0f)
        displayConfig.setFloat("max", (maxValue * displayMultiplier).toFloat())
        displayElement.readConfigTool(displayConfig, player)

        val dotSourceElement = getTopElement(player.worldObj, area, placement.dotSourcePoint) as? ElectricalSourceElement
        if (dotSourceElement == null) {
            return PlacementResult(false, "placed Signal Source but couldn't find top element")
        }
        val dotVoltage = if (dotMask == 0) 0.0 else (dotMask + 0.5) / 256.0 * Eln.SVU
        val dotConfig = NBTTagCompound()
        dotConfig.setDouble("voltage", dotVoltage)
        dotSourceElement.readConfigTool(dotConfig, player)

        messages += "Falstad scope output substituted with Voltage Probe + Digital Display"
        return PlacementResult(true)
    }

    private fun decimalDotMask(decimalPlaces: Int): Int {
        if (decimalPlaces <= 0) return 0
        return 1 shl decimalPlaces
    }

    private fun descriptorFor(kind: FalstadPlacedKind): GenericItemBlockUsingDamageDescriptor? = when (kind) {
        FalstadPlacedKind.RESISTOR -> getBlockDescriptor("Creative Power Resistor")
        FalstadPlacedKind.CAPACITOR -> getBlockDescriptor("Creative Power Capacitor")
        FalstadPlacedKind.INDUCTOR -> getBlockDescriptor("Creative Power Inductor")
        FalstadPlacedKind.DIODE -> getBlockDescriptor("10A Diode")
        FalstadPlacedKind.VOLTAGE_SOURCE -> null
        FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE -> null
        FalstadPlacedKind.CURRENT_SOURCE -> getBlockDescriptor("Current Source")
        FalstadPlacedKind.SWITCH -> getBlockDescriptor("Low Voltage Switch")
        FalstadPlacedKind.PROBE_DISPLAY -> null
    }

    private fun getBlockDescriptor(name: String): GenericItemBlockUsingDamageDescriptor? {
        return GenericItemBlockUsingDamageDescriptor.getByName(name)
    }

    private fun placeSixNode(
        world: World,
        player: EntityPlayerMP,
        area: Area,
        point: FalstadPoint,
        descriptor: GenericItemBlockUsingDamageDescriptor,
        front: LRDU?
    ): PlacementResult {
        val x = area.originX + point.x
        val y = area.groundY
        val z = area.originZ + point.y
        val stack = descriptor.newItemStack(1)
        val placed = Eln.sixNodeItem.onItemUse(stack, player, world, x, y, z, 1, 0.5f, 1.0f, 0.5f)
        if (!placed) {
            val failureSummary = logPlacementFailure(world, player, x, y, z, descriptor, front, stack)
            return PlacementResult(false, failureSummary)
        }

        if (front != null) {
            val element = getTopElement(world, area, point) ?: return PlacementResult(false, "placed block but couldn't find top element")
            element.front = front
            element.sixNode?.reconnect()
            element.needPublish()
        }
        return PlacementResult(true)
    }

    private fun logPlacementFailure(
        world: World,
        player: EntityPlayerMP,
        x: Int,
        y: Int,
        z: Int,
        descriptor: GenericItemBlockUsingDamageDescriptor,
        front: LRDU?,
        stack: ItemStack
    ): String {
        val supportBlock = world.getBlock(x, y, z)
        val supportMeta = world.getBlockMetadata(x, y, z)
        val resolvedX = x
        val resolvedY = if (supportBlock === Blocks.snow_layer && world.getBlockMetadata(x, y, z) and 0x7 < 1) {
            y
        } else if (!supportBlock.isReplaceable(world, x, y, z)) {
            y + 1
        } else {
            y
        }
        val resolvedZ = z
        val targetY = resolvedY
        val targetBlock = world.getBlock(x, targetY, z)
        val targetMeta = world.getBlockMetadata(x, targetY, z)
        val targetTile = world.getTileEntity(x, targetY, z)?.javaClass?.simpleName ?: "none"
        val canEdit = player.canPlayerEdit(x, y, z, 1, stack)
        val canPlaceOnSide = Eln.sixNodeItem.func_150936_a(world, x, y, z, 1, player, stack)
        val supportReplaceable = supportBlock.isReplaceable(world, x, y, z)
        val targetReplaceable = targetBlock.isReplaceable(world, x, targetY, z)
        val sixNodeDescriptor = Eln.sixNodeItem.getDescriptor(stack) as? SixNodeDescriptor
        val placeSide = Direction.fromIntMinecraftSide(1)!!.inverse
        val resolvedFront = sixNodeDescriptor?.getFrontFromPlace(placeSide, player)
        val precheck = if (sixNodeDescriptor == null) {
            "no-sixnode-descriptor"
        } else {
            sixNodeDescriptor.checkCanPlace(Coordinate(resolvedX, resolvedY, resolvedZ, world), placeSide, resolvedFront) ?: "ok"
        }
        Eln.logger.warn(
            "Falstad import placement failed: descriptor='{}' requestedFront={} resolvedFront={} dim={} playerPos=({},{},{}) canEdit={} canPlaceOnSide={} precheck='{}' support=({}, {}, {}) {}:{} solid={} replaceable={} resolvedTarget=({}, {}, {}) {}:{} replaceable={} tile={}",
            descriptor.name,
            front,
            resolvedFront,
            world.provider.dimensionId,
            player.posX,
            player.posY,
            player.posZ,
            canEdit,
            canPlaceOnSide,
            precheck,
            x,
            y,
            z,
            Block.blockRegistry.getNameForObject(supportBlock),
            supportMeta,
            supportBlock.material.isSolid,
            supportReplaceable,
            resolvedX,
            targetY,
            resolvedZ,
            Block.blockRegistry.getNameForObject(targetBlock),
            targetMeta,
            targetReplaceable,
            targetTile
        )
        Eln.logger.info(
            "Falstad import placement failed detail: descriptor='{}' requestedFront={} resolvedFront={} canEdit={} canPlaceOnSide={} precheck='{}' support={} target={} tile={}",
            descriptor.name,
            front,
            resolvedFront,
            canEdit,
            canPlaceOnSide,
            precheck,
            Block.blockRegistry.getNameForObject(supportBlock),
            Block.blockRegistry.getNameForObject(targetBlock),
            targetTile
        )
        return "desc=${descriptor.name}, front=$front->$resolvedFront, canEdit=$canEdit, canPlace=$canPlaceOnSide, precheck=$precheck, target=${Block.blockRegistry.getNameForObject(targetBlock)}, tile=$targetTile"
    }

    private fun getTopElement(world: World, area: Area, point: FalstadPoint): SixNodeElement? {
        val x = area.originX + point.x
        val y = area.groundY + 1
        val z = area.originZ + point.y
        val node = NodeManager.instance?.getNodeFromCoordonate(Coordinate(x, y, z, world)) as? SixNode ?: return null
        return node.getElement(Direction.YN) ?: node.getElement(Direction.YP)
    }

    private fun frontFor(component: FalstadPlacedComponent): LRDU {
        return when (component.kind) {
            FalstadPlacedKind.INDUCTOR -> {
                if (component.axis == FalstadAxis.HORIZONTAL) LRDU.Left else LRDU.Up
            }
            FalstadPlacedKind.RESISTOR, FalstadPlacedKind.CAPACITOR -> {
                if (component.axis == FalstadAxis.HORIZONTAL) LRDU.Left else LRDU.Up
            }
            FalstadPlacedKind.DIODE, FalstadPlacedKind.SWITCH -> {
                if (component.axis == FalstadAxis.HORIZONTAL) {
                    if (component.start.x <= component.end.x) LRDU.Down else LRDU.Up
                } else {
                    if (component.start.y <= component.end.y) LRDU.Left else LRDU.Right
                }
            }
            else -> {
                if (component.axis == FalstadAxis.HORIZONTAL) LRDU.Left else LRDU.Up
            }
        }
    }

    private fun configureElement(
        player: EntityPlayerMP,
        element: SixNodeElement,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        when (element) {
            is CurrentSourceElement -> configureCurrentSource(player, element, component, messages)
            is ElectricalSwitchElement -> configureSwitch(element, component)
            is CreativePowerResistorElement -> configureCreativeResistor(player, element, component, messages)
            is ResistorElement -> configureResistor(element, component, messages)
            is CreativePowerCapacitorElement -> configureCreativeCapacitor(player, element, component, messages)
            is PowerCapacitorSixElement -> {
                val inventory = element.inventory ?: return
                configureCapacitor(inventory, element.descriptor, component, messages)
            }
            is CreativePowerInductorElement -> configureCreativeInductor(player, element, component, messages)
            is PowerInductorSixElement -> {
                val inventory = element.inventory ?: return
                configureInductor(inventory, element.descriptor, component, messages)
            }
        }
    }

    private fun configureCurrentSource(
        player: EntityPlayerMP,
        element: CurrentSourceElement,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val current = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid current value."
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("current", current)
        element.readConfigTool(compound, player)
    }

    private fun configureSwitch(element: ElectricalSwitchElement, component: FalstadPlacedComponent) {
        val rawState = component.source.params.getOrNull(1) ?: component.source.params.getOrNull(0)
        val falstadClosed = when {
            rawState == null -> false
            rawState.equals("true", ignoreCase = true) -> true
            rawState.equals("false", ignoreCase = true) -> false
            else -> (rawState.toIntOrNull() ?: 0) != 0
        }
        element.setSwitchState(!falstadClosed)
    }

    private fun configureResistor(element: ResistorElement, component: FalstadPlacedComponent, messages: MutableList<String>) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid resistance value."
            return
        }
        val inventory = element.inventory ?: return
        val descriptor = element.sixNodeElementDescriptor as? ResistorDescriptor ?: run {
            messages += "Resistor descriptor unavailable."
            return
        }
        val coalDust = GenericItemUsingDamageDescriptor.getByName("Coal Dust") ?: run {
            messages += "Coal Dust descriptor not found."
            return
        }

        var bestCount = 0
        var bestError = Double.POSITIVE_INFINITY
        for (count in 0..64) {
            inventory.setInventorySlotContents(0, if (count == 0) null else coalDust.newItemStack(count))
            val error = abs(descriptor.getRsValue(inventory) - target)
            if (error < bestError) {
                bestError = error
                bestCount = count
            }
        }
        inventory.setInventorySlotContents(0, if (bestCount == 0) null else coalDust.newItemStack(bestCount))
        inventory.markDirty()
    }

    private fun configureCreativeResistor(
        player: EntityPlayerMP,
        element: CreativePowerResistorElement,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid resistance value."
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("resistance", target)
        element.readConfigTool(compound, player)
    }

    private fun configureCapacitor(
        inventory: IInventory,
        descriptor: mods.eln.sixnode.PowerCapacitorSixDescriptor,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid capacitance value."
            return
        }
        val dielectric = GenericItemUsingDamageDescriptor.getByName("Dielectric") ?: run {
            messages += "Dielectric descriptor not found."
            return
        }

        var bestRedstone = 1
        var bestDielectric = 1
        var bestError = Double.POSITIVE_INFINITY
        for (redstone in 1..13) {
            for (dielectricCount in 1..20) {
                inventory.setInventorySlotContents(PowerCapacitorSixContainer.redId, ItemStack(Items.redstone, redstone))
                inventory.setInventorySlotContents(PowerCapacitorSixContainer.dielectricId, dielectric.newItemStack(dielectricCount))
                val error = abs(descriptor.getCValue(inventory) - target)
                if (error < bestError) {
                    bestError = error
                    bestRedstone = redstone
                    bestDielectric = dielectricCount
                }
            }
        }

        inventory.setInventorySlotContents(PowerCapacitorSixContainer.redId, ItemStack(Items.redstone, bestRedstone))
        inventory.setInventorySlotContents(PowerCapacitorSixContainer.dielectricId, dielectric.newItemStack(bestDielectric))
        inventory.markDirty()
    }

    private fun configureCreativeCapacitor(
        player: EntityPlayerMP,
        element: CreativePowerCapacitorElement,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid capacitance value."
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("capacitance", target)
        element.readConfigTool(compound, player)
    }

    private fun configureInductor(
        inventory: IInventory,
        descriptor: mods.eln.sixnode.PowerInductorSixDescriptor,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid inductance value."
            return
        }
        val core = GenericItemUsingDamageDescriptor.getByName("Cheap Ferromagnetic Core")
        val copperCable = GenericItemUsingDamageDescriptor.getByName("Copper Cable")
        if (core == null || copperCable == null) {
            messages += "Inductor support items not found."
            return
        }

        inventory.setInventorySlotContents(PowerInductorSixContainer.coreId, core.newItemStack(1))
        var bestCount = 1
        var bestError = Double.POSITIVE_INFINITY
        for (count in 1..PowerInductorSixContainer.cableStackLimit) {
            inventory.setInventorySlotContents(PowerInductorSixContainer.cableId, copperCable.newItemStack(count))
            val error = abs(descriptor.getlValue(inventory) - target)
            if (error < bestError) {
                bestError = error
                bestCount = count
            }
        }
        inventory.setInventorySlotContents(PowerInductorSixContainer.cableId, copperCable.newItemStack(bestCount))
        inventory.markDirty()
    }

    private fun configureCreativeInductor(
        player: EntityPlayerMP,
        element: CreativePowerInductorElement,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += "Line ${component.source.lineNumber}: invalid inductance value."
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("inductance", target)
        element.readConfigTool(compound, player)
    }

    private fun findPlacementArea(world: World, player: EntityPlayerMP, width: Int, height: Int): Area? {
        val centerX = floor(player.posX).toInt()
        val centerZ = floor(player.posZ).toInt()
        val baseY = floor(player.posY).toInt() - 1

        for (radius in 2..20) {
            for (dz in -radius..radius) {
                for (dx in -radius..radius) {
                    val originX = centerX + dx
                    val originZ = centerZ + dz
                    val groundY = findGroundY(world, originX, originZ, baseY) ?: continue
                    if (isFlatClearArea(world, originX, groundY, originZ, width, height)) {
                        return Area(originX, groundY, originZ)
                    }
                }
            }
        }
        return null
    }

    private fun findGroundY(world: World, x: Int, z: Int, preferredY: Int): Int? {
        for (y in preferredY + 3 downTo maxOf(1, preferredY - 8)) {
            val block = world.getBlock(x, y, z)
            if (isSolidSupport(world, x, y, z, block)) {
                return y
            }
        }
        return null
    }

    private fun isFlatClearArea(world: World, originX: Int, groundY: Int, originZ: Int, width: Int, height: Int): Boolean {
        for (x in originX until originX + width) {
            for (z in originZ until originZ + height) {
                val block = world.getBlock(x, groundY, z)
                if (!isSolidSupport(world, x, groundY, z, block)) return false
                if (!isReplaceableAbove(world, x, groundY + 1, z)) return false
                if (world.getTileEntity(x, groundY + 1, z) != null) return false
            }
        }
        return true
    }

    private fun isSolidSupport(world: World, x: Int, y: Int, z: Int, block: Block): Boolean {
        return block !== Blocks.air && block.material.isSolid && !block.isReplaceable(world, x, y, z)
    }

    private fun isReplaceableAbove(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlock(x, y, z)
        return block === Blocks.air || block.isReplaceable(world, x, y, z)
    }
}
