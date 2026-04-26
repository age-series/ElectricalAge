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
import mods.eln.sixnode.ElectricalVuMeterDescriptor
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor
import mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayElement
import mods.eln.sixnode.electricalsensor.ElectricalSensorElement
import mods.eln.sixnode.electricalsource.ElectricalSourceElement
import mods.eln.sixnode.electricalswitch.ElectricalSwitchElement
import mods.eln.sixnode.logicgate.LogicGateDescriptor
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
import mods.eln.i18n.I18N.tr
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor

object FalstadImporter {
    private const val DEFAULT_FALSTAD_CURRENT_SOURCE_AMPS = 0.01
    private const val SEARCH_RADIUS = 20
    private data class Area(val originX: Int, val groundY: Int, val originZ: Int)
    private data class Footprint(val width: Int, val height: Int) {
        val area: Int get() = width * height
    }
    private data class PlannedPlacement(val area: Area, val plan: FalstadLayoutPlan, val rotated: Boolean)
    private data class PlacementSearchResult(val placement: PlannedPlacement?, val bestNearbyArea: Footprint?)
    private data class VoltageSourcePlacement(val sourcePoint: FalstadPoint, val groundPoint: FalstadPoint, val voltage: Double)
    private data class CurrentSourcePlacement(val sourcePoint: FalstadPoint, val groundPoint: FalstadPoint, val current: Double)
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
                            addChatMessage(livePlayer, tr("Falstad import: clipboard is not valid Falstad data."))
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
            addChatMessage(player, tr("Falstad import: no components found."))
            return
        }

        addChatMessage(player, tr("Falstad import: requires a flat, clear area of %1$. Planned orientation: original.", requiredAreaText(plan)))

        val search = findPlacementArea(player.worldObj, player, plan)
        val placement = search.placement
        if (placement == null) {
            val bestNearby = search.bestNearbyArea?.let { " " + tr($$"Largest nearby area found was %1$x%2$ blocks.", it.width, it.height) } ?: ""
            addChatMessage(player, tr("Falstad import: couldn't find a flat, clear area near the player for %1$.%2$", requiredAreaText(plan), bestNearby))
            return
        }
        val orientation = if (placement.rotated) tr("rotated clockwise") else tr("original")
        addChatMessage(player, tr($$"Falstad import: using %1$ placement (%2$x%3$).", orientation, placement.plan.width, placement.plan.height))

        val placedPlan = placement.plan
        val area = placement.area
        val messages = mutableListOf<String>()
        val useSignalCables = placedPlan.components.any {
            it.kind == FalstadPlacedKind.FALSTAD_AND_GATE ||
            it.kind == FalstadPlacedKind.FALSTAD_NAND_GATE ||
                it.kind == FalstadPlacedKind.FALSTAD_OR_GATE ||
                it.kind == FalstadPlacedKind.FALSTAD_NOR_GATE ||
                it.kind == FalstadPlacedKind.FALSTAD_XOR_GATE ||
                it.kind == FalstadPlacedKind.FALSTAD_NOT_GATE ||
                it.kind == FalstadPlacedKind.SIGNAL_INPUT ||
                it.kind == FalstadPlacedKind.SIGNAL_OUTPUT
        }
        val normalCableDescriptor = if (useSignalCables) Eln.instance.signalCableDescriptor else Eln.instance.lowVoltageCableDescriptor
        val probeDisplayMax = probeDisplayMaxValue(placedPlan)
        val sourcedByVoltageSource = placedPlan.components
            .mapNotNull {
                when (it.kind) {
                    FalstadPlacedKind.VOLTAGE_SOURCE -> voltageSourcePlacement(it).sourcePoint
                    FalstadPlacedKind.CURRENT_SOURCE -> currentSourcePlacement(it).sourcePoint
                    FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE -> singlePortVoltageSourcePlacement(it).sourcePoint
                    else -> null
                }
            }
            .toSet()
        val groundedByVoltageSource = placedPlan.components
            .mapNotNull {
                when (it.kind) {
                    FalstadPlacedKind.VOLTAGE_SOURCE -> voltageSourcePlacement(it).groundPoint
                    FalstadPlacedKind.CURRENT_SOURCE -> currentSourcePlacement(it).groundPoint
                    else -> null
                }
            }
            .toSet()

        for ((point, kind) in placedPlan.nodes) {
            if (point in sourcedByVoltageSource) {
                continue
            }
            val descriptor = when (kind) {
                FalstadNodeKind.GROUND -> getBlockDescriptor("Ground Cable")
                FalstadNodeKind.NORMAL -> if (point in groundedByVoltageSource) getBlockDescriptor("Ground Cable") else normalCableDescriptor
            } ?: continue

            val result = placeSixNode(player.worldObj, player, area, point, descriptor, null)
            if (!result.placed) {
                messages += tr("Failed to place %1$ at %2$,%3$", descriptor.name, point.x, point.y)
                result.failureSummary?.let { messages += tr("Debug %1$ at %2$,%3$: %4$", descriptor.name, point.x, point.y, it) }
            }
        }

        for (point in placedPlan.wires) {
            val result = placeSixNode(player.worldObj, player, area, point, normalCableDescriptor, null)
            if (!result.placed) {
                messages += tr("Failed to place wire at %1$,%2$", point.x, point.y)
                result.failureSummary?.let { messages += tr("Debug wire at %1$,%2$: %3$", point.x, point.y, it) }
            }
        }

        for (component in placedPlan.components) {
            if (component.kind == FalstadPlacedKind.VOLTAGE_SOURCE) {
                val result = placeVoltageSource(player, area, component, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad voltage source at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad voltage source at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                messages += tr("Voltage source substituted with Electrical Source + Ground")
                continue
            }
            if (component.kind == FalstadPlacedKind.SINGLE_PORT_VOLTAGE_SOURCE) {
                val result = placeSinglePortVoltageSource(player, area, component, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad voltage source at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad voltage source at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                messages += tr("Adjustable voltage source substituted with Electrical Source")
                continue
            }
            if (component.kind == FalstadPlacedKind.CURRENT_SOURCE) {
                val result = placeCurrentSource(player, area, component, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad current source at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad current source at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                messages += tr("Current source substituted with Current Source + Ground")
                continue
            }
            if (component.kind == FalstadPlacedKind.SIGNAL_INPUT) {
                val result = placeSignalInput(player, area, component, placement.rotated, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad logic input at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad logic input at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                continue
            }
            if (component.kind == FalstadPlacedKind.SIGNAL_OUTPUT) {
                val result = placeSignalOutput(player, area, component, placement.rotated, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad logic output at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad logic output at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                continue
            }
            if (
                component.kind == FalstadPlacedKind.FALSTAD_AND_GATE ||
                component.kind == FalstadPlacedKind.FALSTAD_NAND_GATE ||
                component.kind == FalstadPlacedKind.FALSTAD_OR_GATE ||
                component.kind == FalstadPlacedKind.FALSTAD_NOR_GATE ||
                component.kind == FalstadPlacedKind.FALSTAD_XOR_GATE ||
                component.kind == FalstadPlacedKind.FALSTAD_NOT_GATE
            ) {
                val result = placeFalstadNandGate(player, area, component, placement.rotated, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad gate at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad gate at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                continue
            }
            if (component.kind == FalstadPlacedKind.PROBE_DISPLAY) {
                val result = placeProbeDisplay(player, area, component, probeDisplayMax, messages)
                if (!result.placed) {
                    messages += tr("Failed to place Falstad probe/display at %1$,%2$", component.cell.x, component.cell.y)
                    result.failureSummary?.let { messages += tr("Debug Falstad probe/display at %1$,%2$: %3$", component.cell.x, component.cell.y, it) }
                }
                continue
            }

            val descriptor = descriptorFor(component.kind)
            if (descriptor == null) {
                messages += tr("Missing ELN descriptor for %1$", component.kind)
                continue
            }
            val result = placeSixNode(player.worldObj, player, area, component.cell, descriptor, frontFor(component, placement.rotated))
            if (!result.placed) {
                messages += tr("Failed to place %1$ at %2$,%3$", descriptor.name, component.cell.x, component.cell.y)
                result.failureSummary?.let { messages += tr("Debug %1$ at %2$,%3$: %4$", descriptor.name, component.cell.x, component.cell.y, it) }
                continue
            }
            val element = getTopElement(player.worldObj, area, component.cell)
            if (element != null) {
                configureElement(player, element, component, messages)
            }
            messages += component.substitutions
        }

        val totalPlaced = placedPlan.nodes.size + placedPlan.wires.size + placedPlan.components.size
        addChatMessage(player, tr("Falstad import: placed %1$ blocks.", totalPlaced))
        for (message in placedPlan.warnings.distinct().plus(messages.distinct()).distinct().take(8)) {
            addChatMessage(player, tr("Falstad import: %1$", message))
        }
        if (placedPlan.warnings.distinct().plus(messages.distinct()).distinct().size > 8) {
            addChatMessage(player, tr("Falstad import: additional warnings omitted."))
        }
    }

    private fun requiredAreaText(plan: FalstadLayoutPlan): String {
        return if (plan.width == plan.height) {
            tr($$"%1$x%2$ blocks", plan.width, plan.height)
        } else {
            tr($$"%1$x%2$ blocks (%3$x%4$ rotated also works)", plan.width, plan.height, plan.height, plan.width)
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

    private fun currentSourcePlacement(component: FalstadPlacedComponent): CurrentSourcePlacement {
        val rawCurrent = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: DEFAULT_FALSTAD_CURRENT_SOURCE_AMPS
        return if (rawCurrent >= 0.0) {
            CurrentSourcePlacement(component.end, component.start, rawCurrent)
        } else {
            CurrentSourcePlacement(component.start, component.end, -rawCurrent)
        }
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
        val descriptor = getBlockDescriptor("Electrical Source") ?: return PlacementResult(false, tr("missing descriptor: Electrical Source"))
        val placement = voltageSourcePlacement(component)
        val placed = placeSixNode(player.worldObj, player, area, placement.sourcePoint, descriptor, frontFor(component))
        if (!placed.placed) return placed

        val element = getTopElement(player.worldObj, area, placement.sourcePoint) as? ElectricalSourceElement
        if (element == null) {
            messages += tr("Voltage source placed, but couldn't configure its voltage.")
            return PlacementResult(true)
        }

        val compound = NBTTagCompound()
        compound.setDouble("voltage", placement.voltage)
        element.readConfigTool(compound, player)

        val waveform = component.source.params.getOrNull(1)?.toIntOrNull() ?: 0
        val frequency = component.source.params.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        if (waveform != 0 || abs(frequency) > 0.000001) {
            messages += tr("Line %1$: non-DC source imported as DC.", component.source.lineNumber)
        }
        return PlacementResult(true)
    }

    private fun placeSinglePortVoltageSource(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = getBlockDescriptor("Electrical Source") ?: return PlacementResult(false, tr("missing descriptor: Electrical Source"))
        val placement = singlePortVoltageSourcePlacement(component)
        val placed = placeSixNode(player.worldObj, player, area, placement.sourcePoint, descriptor, frontFor(component))
        if (!placed.placed) return placed

        val element = getTopElement(player.worldObj, area, placement.sourcePoint) as? ElectricalSourceElement
        if (element == null) {
            messages += tr("Voltage source placed, but couldn't configure its voltage.")
            return PlacementResult(true)
        }

        val compound = NBTTagCompound()
        compound.setDouble("voltage", placement.voltage)
        element.readConfigTool(compound, player)
        return PlacementResult(true)
    }

    private fun placeCurrentSource(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = getBlockDescriptor("Current Source") ?: return PlacementResult(false, tr("missing descriptor: Current Source"))
        val placement = currentSourcePlacement(component)
        val placed = placeSixNode(player.worldObj, player, area, placement.sourcePoint, descriptor, frontFor(component))
        if (!placed.placed) return placed

        val element = getTopElement(player.worldObj, area, placement.sourcePoint) as? CurrentSourceElement
        if (element == null) {
            messages += tr("Current source placed, but couldn't configure its current.")
            return PlacementResult(true)
        }

        val compound = NBTTagCompound()
        compound.setDouble("current", placement.current)
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
        val probeDescriptor = getBlockDescriptor("Voltage Probe") ?: return PlacementResult(false, tr("missing descriptor: Voltage Probe"))
        val displayDescriptor = getBlockDescriptor("Digital Display") ?: return PlacementResult(false, tr("missing descriptor: Digital Display"))
        val signalSourceDescriptor = getBlockDescriptor("Signal Source") ?: return PlacementResult(false, tr("missing descriptor: Signal Source"))
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
            return PlacementResult(false, tr("placed Voltage Probe but couldn't find top element"))
        }
        probeElement.inventory?.setInventorySlotContents(0, Eln.instance.lowVoltageCableDescriptor.newItemStack(1))
        probeElement.inventory?.markDirty()
        val probeConfig = NBTTagCompound()
        probeConfig.setFloat("min", 0.0f)
        probeConfig.setFloat("max", maxValue.toFloat())
        probeElement.readConfigTool(probeConfig, player)

        val displayElement = getTopElement(player.worldObj, area, placement.displayPoint) as? ElectricalDigitalDisplayElement
        if (displayElement == null) {
            return PlacementResult(false, tr("placed Digital Display but couldn't find top element"))
        }
        val displayConfig = NBTTagCompound()
        displayConfig.setFloat("min", 0.0f)
        displayConfig.setFloat("max", (maxValue * displayMultiplier).toFloat())
        displayElement.readConfigTool(displayConfig, player)

        val dotSourceElement = getTopElement(player.worldObj, area, placement.dotSourcePoint) as? ElectricalSourceElement
        if (dotSourceElement == null) {
            return PlacementResult(false, tr("placed Signal Source but couldn't find top element"))
        }
        val dotVoltage = if (dotMask == 0) 0.0 else (dotMask + 0.5) / 256.0 * Eln.SVU
        val dotConfig = NBTTagCompound()
        dotConfig.setDouble("voltage", dotVoltage)
        dotSourceElement.readConfigTool(dotConfig, player)

        messages += tr("Falstad scope output substituted with Voltage Probe + Digital Display")
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
        FalstadPlacedKind.FALSTAD_AND_GATE -> getBlockDescriptor("AND Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.FALSTAD_NAND_GATE -> getBlockDescriptor("NAND Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.FALSTAD_OR_GATE -> getBlockDescriptor("OR Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.FALSTAD_NOR_GATE -> getBlockDescriptor("NOR Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.FALSTAD_XOR_GATE -> getBlockDescriptor("XOR Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.FALSTAD_NOT_GATE -> getBlockDescriptor("NOT Chip", LogicGateDescriptor::class.java)
        FalstadPlacedKind.SIGNAL_INPUT -> getBlockDescriptor("Signal Switch", ElectricalGateSourceDescriptor::class.java)
        FalstadPlacedKind.SIGNAL_OUTPUT -> getBlockDescriptor("LED vuMeter", ElectricalVuMeterDescriptor::class.java)
    }

    private fun getBlockDescriptor(name: String): GenericItemBlockUsingDamageDescriptor? {
        return GenericItemBlockUsingDamageDescriptor.getByName(name)
    }

    private fun getBlockDescriptor(
        name: String,
        descriptorClass: Class<out GenericItemBlockUsingDamageDescriptor>
    ): GenericItemBlockUsingDamageDescriptor? {
        return Eln.sixNodeItem.descriptors.firstOrNull { it != null && it.name == name && descriptorClass.isInstance(it) }
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
            val element = getTopElement(world, area, point) ?: return PlacementResult(false, tr("placed block but couldn't find top element"))
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

    private fun frontToward(cell: FalstadPoint, target: FalstadPoint): LRDU {
        return when {
            target.x > cell.x -> LRDU.Right
            target.x < cell.x -> LRDU.Left
            target.y > cell.y -> LRDU.Down
            target.y < cell.y -> LRDU.Up
            else -> LRDU.Up
        }
    }

    private fun frontFor(component: FalstadPlacedComponent, rotatedPlacement: Boolean = false): LRDU {
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
            FalstadPlacedKind.FALSTAD_AND_GATE,
            FalstadPlacedKind.FALSTAD_NAND_GATE,
            FalstadPlacedKind.FALSTAD_OR_GATE,
            FalstadPlacedKind.FALSTAD_NOR_GATE,
            FalstadPlacedKind.FALSTAD_XOR_GATE,
            FalstadPlacedKind.FALSTAD_NOT_GATE,
            FalstadPlacedKind.SIGNAL_INPUT,
            FalstadPlacedKind.SIGNAL_OUTPUT -> {
                val baseFront = frontToward(component.cell, component.extraPoints.firstOrNull() ?: component.start)
                when (component.kind) {
                    FalstadPlacedKind.FALSTAD_AND_GATE,
                    FalstadPlacedKind.FALSTAD_NAND_GATE,
                    FalstadPlacedKind.FALSTAD_OR_GATE,
                    FalstadPlacedKind.FALSTAD_NOR_GATE,
                    FalstadPlacedKind.FALSTAD_XOR_GATE,
                    FalstadPlacedKind.FALSTAD_NOT_GATE,
                    FalstadPlacedKind.SIGNAL_INPUT,
                    FalstadPlacedKind.SIGNAL_OUTPUT -> if (rotatedPlacement) baseFront.right() else baseFront.left()
                    else -> baseFront
                }
            }
            else -> {
                if (component.axis == FalstadAxis.HORIZONTAL) LRDU.Left else LRDU.Up
            }
        }
    }

    private fun placeSignalInput(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        rotatedPlacement: Boolean,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = descriptorFor(component.kind) ?: return PlacementResult(false, tr("missing descriptor: Signal Switch"))
        val result = placeSixNode(player.worldObj, player, area, component.cell, descriptor, frontFor(component, rotatedPlacement))
        if (!result.placed) return result

        val element = getTopElement(player.worldObj, area, component.cell) as? ElectricalGateSourceElement
        if (element == null) {
            messages += tr("Signal switch placed, but couldn't configure its state.")
            return PlacementResult(true)
        }
        element.outputGateProcess.state(component.forcedSwitchState ?: false)
        element.needPublish()
        messages += component.substitutions
        return PlacementResult(true)
    }

    private fun placeSignalOutput(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        rotatedPlacement: Boolean,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = descriptorFor(component.kind) ?: return PlacementResult(false, tr("missing descriptor: LED vuMeter"))
        val result = placeSixNode(player.worldObj, player, area, component.cell, descriptor, frontFor(component, rotatedPlacement))
        if (result.placed) messages += component.substitutions
        return result
    }

    private fun placeFalstadNandGate(
        player: EntityPlayerMP,
        area: Area,
        component: FalstadPlacedComponent,
        rotatedPlacement: Boolean,
        messages: MutableList<String>
    ): PlacementResult {
        val descriptor = descriptorFor(component.kind) ?: return PlacementResult(false, tr("missing descriptor: NAND Chip"))
        val result = placeSixNode(player.worldObj, player, area, component.cell, descriptor, frontFor(component, rotatedPlacement))
        if (result.placed) messages += component.substitutions
        return result
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
            messages += tr("Line %1$: invalid current value.", component.source.lineNumber)
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("current", current)
        element.readConfigTool(compound, player)
    }

    private fun configureSwitch(element: ElectricalSwitchElement, component: FalstadPlacedComponent) {
        val falstadClosed = component.forcedSwitchState ?: run {
            val rawState = component.source.params.getOrNull(1) ?: component.source.params.getOrNull(0)
            when {
                rawState == null -> false
                rawState.equals("true", ignoreCase = true) -> true
                rawState.equals("false", ignoreCase = true) -> false
                else -> (rawState.toIntOrNull() ?: 0) != 0
            }
        }
        element.setSwitchState(!falstadClosed)
    }

    private fun configureResistor(element: ResistorElement, component: FalstadPlacedComponent, messages: MutableList<String>) {
        val target = component.source.params.getOrNull(1)?.toDoubleOrNull() ?: run {
            messages += tr("Line %1$: invalid resistance value.", component.source.lineNumber)
            return
        }
        val inventory = element.inventory ?: return
        val descriptor = element.sixNodeElementDescriptor as? ResistorDescriptor ?: run {
            messages += tr("Resistor descriptor unavailable.")
            return
        }
        val coalDust = GenericItemUsingDamageDescriptor.getByName("Coal Dust") ?: run {
            messages += tr("Coal Dust descriptor not found.")
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
            messages += tr("Line %1$: invalid resistance value.", component.source.lineNumber)
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
            messages += tr("Line %1$: invalid capacitance value.", component.source.lineNumber)
            return
        }
        val dielectric = GenericItemUsingDamageDescriptor.getByName("Dielectric") ?: run {
            messages += tr("Dielectric descriptor not found.")
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
            messages += tr("Line %1$: invalid capacitance value.", component.source.lineNumber)
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
            messages += tr("Line %1$: invalid inductance value.", component.source.lineNumber)
            return
        }
        val core = GenericItemUsingDamageDescriptor.getByName("Cheap Ferromagnetic Core")
        val copperCable = GenericItemUsingDamageDescriptor.getByName("Copper Cable")
        if (core == null || copperCable == null) {
            messages += tr("Inductor support items not found.")
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
            messages += tr("Line %1$: invalid inductance value.", component.source.lineNumber)
            return
        }
        val compound = NBTTagCompound()
        compound.setDouble("inductance", target)
        element.readConfigTool(compound, player)
    }

    private fun findPlacementArea(world: World, player: EntityPlayerMP, plan: FalstadLayoutPlan): PlacementSearchResult {
        val centerX = floor(player.posX).toInt()
        val centerZ = floor(player.posZ).toInt()
        val baseY = floor(player.posY).toInt() - 1
        val rotatedPlan = if (plan.width != plan.height) plan.rotatedClockwise() else null
        var bestNearbyArea: Footprint? = null

        for (radius in 2..SEARCH_RADIUS) {
            for (dz in -radius..radius) {
                for (dx in -radius..radius) {
                    val originX = centerX + dx
                    val originZ = centerZ + dz
                    val groundY = findGroundY(world, originX, originZ, baseY) ?: continue
                    val measured = measureClearFootprint(
                        world,
                        originX,
                        groundY,
                        originZ,
                        centerX + SEARCH_RADIUS,
                        centerZ + SEARCH_RADIUS
                    )
                    if (measured.area > (bestNearbyArea?.area ?: 0)) {
                        bestNearbyArea = measured
                    }
                    if (isFlatClearArea(world, originX, groundY, originZ, plan.width, plan.height)) {
                        return PlacementSearchResult(PlannedPlacement(Area(originX, groundY, originZ), plan, rotated = false), bestNearbyArea)
                    }
                    if (rotatedPlan != null && isFlatClearArea(world, originX, groundY, originZ, rotatedPlan.width, rotatedPlan.height)) {
                        return PlacementSearchResult(PlannedPlacement(Area(originX, groundY, originZ), rotatedPlan, rotated = true), bestNearbyArea)
                    }
                }
            }
        }
        return PlacementSearchResult(null, bestNearbyArea)
    }

    private fun measureClearFootprint(world: World, originX: Int, groundY: Int, originZ: Int, maxX: Int, maxZ: Int): Footprint {
        var width = 0
        while (originX + width <= maxX && isClearTile(world, originX + width, groundY, originZ)) {
            width++
        }

        var height = 0
        heightLoop@ while (originZ + height <= maxZ) {
            for (xOffset in 0 until width) {
                if (!isClearTile(world, originX + xOffset, groundY, originZ + height)) {
                    break@heightLoop
                }
            }
            height++
        }

        return Footprint(width, height)
    }

    private fun isClearTile(world: World, x: Int, groundY: Int, z: Int): Boolean {
        val block = world.getBlock(x, groundY, z)
        return isSolidSupport(world, x, groundY, z, block) &&
            isReplaceableAbove(world, x, groundY + 1, z) &&
            world.getTileEntity(x, groundY + 1, z) == null
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
                if (!isClearTile(world, x, groundY, z)) return false
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
