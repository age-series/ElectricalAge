package mods.eln.transparentnode.autominer;

import mods.eln.Eln;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

public class AutoMinerRender extends TransparentNodeElementRender {
    private final AutoMinerDescriptor descriptor;
    private final float[] buttonsState;
    private final boolean[] ledsAState;
    private final boolean[] ledsPState;

    private final RenderStorage render = new RenderStorage(Eln.instance.autominerRange, 130, 24, 24);

    private final PhysicalInterpolatorNoRebound pipeLengthInterpol = new PhysicalInterpolatorNoRebound(0.4f, 2f, 0.8f);
    private final RcInterpolator rotSpeed = new RcInterpolator(1);
    private float pipeAlpha = 0;

    private final LinkedList<String> logs = new LinkedList<String>();

    private final int logSizeMax = 9;

    private boolean powerOk;

    private float recalcTimeout = 0;

    private final TransparentNodeElementInventory inventory =
        new TransparentNodeElementInventory(AutoMinerContainer.inventorySize, 64, this);

    private short pipeLength = 0;
    AutoMinerSlowProcess.jobType job;
    private boolean boot = true;
    public boolean silkTouch = false;

    public AutoMinerRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (AutoMinerDescriptor) descriptor;

        buttonsState = new float[this.descriptor.buttonsCount];
        for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
            buttonsState[idx] = (float) Math.random();
        }

        ledsAState = new boolean[this.descriptor.ledsACount];
        for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
            ledsAState[idx] = Math.random() > 0.5;
        }

        ledsPState = new boolean[this.descriptor.ledsPCount];
        for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
            ledsPState[idx] = Math.random() > 0.5;
        }

        addLoopedSound(new LoopedSound("eln:autominer", coordinate(), ISound.AttenuationType.LINEAR) {
            @Override
            public float getVolume() {
                if (powerOk &&
                    (job == AutoMinerSlowProcess.jobType.ore || job == AutoMinerSlowProcess.jobType.pipeAdd ||
                        job == AutoMinerSlowProcess.jobType.pipeRemove))
                    return 2.13f;
                else
                    return 0f;
            }
        });
    }

    private void pushLog(String string) {
        logs.addFirst(string);
        if (logs.size() > logSizeMax)
            logs.removeLast();
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        GL11.glRotatef(pipeAlpha, 0, -1, 0);
        GL11.glPushMatrix();
        GL11.glScalef(0.99f, 0.99f, 0.99f);
        descriptor.pipe.draw();
        GL11.glPopMatrix();

        int len = (int) pipeLengthInterpol.get();
        GL11.glTranslatef(0, -(pipeLengthInterpol.get() - len), 0);
        for (int idx = len + 2; idx != 0; idx--) {
            if (idx != 1) {
                descriptor.pipe.draw();
            } else {
                descriptor.head.draw();
            }
            GL11.glTranslatef(0, -1f, 0);
        }
        GL11.glPopMatrix();

        for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
            buttonsState[idx] = idx == job.ordinal() && powerOk ? 1 : 0;
        }

        front.glRotateXnRef();

        boolean drawScreen = UtilsClient.clientDistanceTo(getTileEntity()) < 20 && powerOk;
        boolean drawRay = drawScreen && job != null;

        UtilsClient.disableCulling();

        GL11.glPushMatrix();
        GL11.glTranslatef(-1.57031f, 1.8125f - 1.5f - 0.02f, -0.3125f + 0.02f);
        GL11.glRotatef(90, 0, 1, 0);

        if (drawScreen) {
            UtilsClient.disableLight();
            GL11.glPushMatrix();
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glScalef(1 / 128f, -1 / 128f, 1);
            int idx = 0;
            for (String log : logs) {
                Minecraft.getMinecraft().fontRenderer.drawString(idx == 0 ? FC.BRIGHT_GREEN + "> " +
                    log.substring(2) : FC.DARK_GREEN + log, 80, 1 + idx, 0xFFD0D0D0 /*No effect...*/);
                idx += 8;
            }
            GL11.glPopMatrix();
            UtilsClient.enableLight();
        }

        if (drawRay) {
            float raySize = 0.625f - 0.02f * 2;
            float scale = 1f / render.resWidth * raySize;

            GL11.glTranslatef(-raySize, 0, 0);
            GL11.glScalef(scale, -scale, 1);
            render.draw(0.4f, 1.3f, 0.8f);
        }

        GL11.glPopMatrix();
        UtilsClient.enableCulling();

        descriptor.draw(false, buttonsState, ledsAState, ledsPState);
    }

    public void refresh(float deltaT) {
        super.refresh(deltaT);
        pipeAlpha += rotSpeed.get() * deltaT;
        if (pipeAlpha > 360) pipeAlpha -= 360;
        for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
            if (powerOk) {
                if (Math.random() < 0.2 * deltaT)
                    ledsAState[idx] = !ledsAState[idx];
            } else {
                ledsAState[idx] = true;
            }
        }

        for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
            if (powerOk) {
                if (Math.random() < 0.2 * deltaT)
                    ledsPState[idx] = !ledsPState[idx];
            } else {
                ledsPState[idx] = true;
            }
        }

        if (powerOk) {
            recalcTimeout -= deltaT;
            if (recalcTimeout < 0) {
                recalcTimeout += 0.5;
                float camAlpha;
                switch (front) {
                    default:
                    case XN:
                        camAlpha = (float) Math.PI;
                        break;
                    case XP:
                        camAlpha = 0;
                        break;
                    case ZN:
                        camAlpha = (float) (-Math.PI / 2);
                        break;
                    case ZP:
                        camAlpha = (float) (Math.PI / 2);
                        break;
                }
                render.generate(this.getTileEntity().getWorldObj(), getTileEntity().xCoord + 0.5,
                    getTileEntity().yCoord + 0.5 - (Math.max(0, pipeLength - 5)),
                    getTileEntity().zCoord + 0.5, -(float) (Math.PI * 1 / 2) + camAlpha, -(float) (Math.PI / 2));
            }
        }

        pipeLengthInterpol.step(deltaT);
        if (pipeLengthInterpol.get() < 0) pipeLengthInterpol.setPos(0);
        rotSpeed.step(deltaT);
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new AutoMinerGuiDraw(player, inventory, this);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            if (pipeLength != (pipeLength = stream.readShort())) {
                recalcTimeout = 0;
            }
            if (job != (job = AutoMinerSlowProcess.jobType.values()[stream.readByte()])) {
                switch (job) {
                    case ore:
                        rotSpeed.setTarget(360);
                        break;
                    default:
                        rotSpeed.setTarget(0);
                        break;
                }
            }
            powerOk = stream.readBoolean();
            silkTouch = stream.readBoolean();

            if (!powerOk) {
                logs.clear();
                pushLog("[i] Boot... Done.");
                pushLog("* Waiting opcode.");
            }

            if (boot) {
                boot = false;
                pipeLengthInterpol.setPos(pipeLength);
            }
            pipeLengthInterpol.setTarget(pipeLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            switch (stream.readByte()) {
                case AutoMinerElement.pushLogId:
                    pushLog(stream.readUTF());
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
