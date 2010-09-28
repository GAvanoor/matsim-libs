/**
 * <h2>Usage restrictions:</h2>
 * <ul>
 *   <li> Do not use, yet!</li>
 * </ul>
 * 
 * TODO update documentation 
 * <h2>Signal Control</h2>
 * <ul>
 *   <li>SignalSystemController implementations are responsible to throw the
 *   appropriate SignalGroupStateChangedEvents.</li>
 *   <li>SingalSystemController used by the QSim of MATSim are automatically
 *   registered as a SimulationListener if one of the SimulationListener interfaces
 *   is implemented<li>
 * </ul>
 * 
 * <h3>Plan Based Signal Control<h3>
 * Plan based signal control uses the attributes 
 * <ul>
 *   <li>cycle time (ct)</li>
 *   <li>roughcast (rc)</li>
 *   <li>dropping (dr)</li>
 *   <li>intergreen time roughcast (igrc)</li>
 *   <li>intergreen time dropping (igdr)</li>
 * </ul>
 * to determine the state of a signal group by the following schemes:
 * 
 * If rc <= dr:
 * <p>
 *  |__red___|_redyellow_|_green_____________|_yellow_|_red______|  <br>
 *  |------------| ---------------|-------------------------------|------------|----------------|  <br>
 *  0_______rc_________rc+igrc______________dr______dr+igdr____ct <br>
 * <p>
 * if dr < rc:
 * <p>
 *  |_green___|_yellow____|_red______________|_redyellow_|_green_|  <br>
 *  |---------------|_---------------|----------------------------|-----------------|-----------_|  <br>
 *  0_________dr_________dr+igdr____________rc________rc+igrc___ct <br>
 * <p>
 * 
 * At which state vehicles moved is responsibility of the SignalSystemControler.
 * 
 *	<h2>Package Maintainer(s):</h2>
 *		<ul>
 *			<li>Dominik Grether</li>
 * 		</ul>
 * 
 *	Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 *	@author dgrether
 */
package org.matsim.signalsystems.model;