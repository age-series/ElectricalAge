package li.cil.oc.api.internal;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import net.minecraft.inventory.IInventory;

/**
 * This interface is implemented as a marker by computer cases.
 * <p/>
 * This is implemented by computer case tile entities. That means you can
 * use this to check for computer cases by using:
 * <pre>
 *     if (tileEntity instanceof Case) {
 * </pre>
 * <p/>
 * The only purpose is to allow identifying tile entities as computer cases
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Case extends Environment, EnvironmentHost, Colored, Rotatable, Tiered, IInventory {
    /**
     * The machine currently hosted by this computer case.
     */
    Machine machine();
}
