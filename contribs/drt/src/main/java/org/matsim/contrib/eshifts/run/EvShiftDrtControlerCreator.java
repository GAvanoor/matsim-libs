package org.matsim.contrib.eshifts.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.eshifts.charging.ShiftOperatingVehicleProvider;
import org.matsim.contrib.eshifts.fleet.EvShiftDvrpFleetQSimModule;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.shifts.io.DrtShiftsReader;
import org.matsim.contrib.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilitiesUtils;
import org.matsim.contrib.shifts.shift.DrtShiftUtils;
import org.matsim.contrib.shifts.shift.DrtShifts;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author nkuehnel, fzwick
 */
public class EvShiftDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);
		final OperationFacilities operationFacilities = OperationFacilitiesUtils.getOrCreateShifts(scenario);
		if(shiftDrtConfigGroup.getOperationFacilityInputFile() != null) {
			new OperationFacilitiesReader(operationFacilities).readFile(shiftDrtConfigGroup.getOperationFacilityInputFile());
		}

		final DrtShifts shifts = DrtShiftUtils.getOrCreateShifts(scenario);
		if(shiftDrtConfigGroup.getShiftInputFile() != null) {
			new DrtShiftsReader(shifts).readFile(shiftDrtConfigGroup.getShiftInputFile());
		}

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new MultiModeShiftEDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EvModule());

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new EvShiftDvrpFleetQSimModule(drtCfg.getMode()));
		}

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(AuxDischargingHandler.VehicleProvider.class).to(ShiftOperatingVehicleProvider.class);
			}
		});

		controler.configureQSimComponents(DvrpQSimComponents.activateModes(List.of(EvModule.EV_COMPONENT, "SHIFT_COMPONENT"),
				multiModeDrtConfig.modes().collect(toList())));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
