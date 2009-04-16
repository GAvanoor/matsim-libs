/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.dgrether.daganzosignal;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.basic.network.BasicLane;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.network.BasicLaneDefinitionsBuilder;
import org.matsim.core.basic.network.BasicLanesToLinkAssignment;
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystems.BasicSignalSystemsBuilder;
import org.matsim.core.basic.signalsystemsconfig.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurationsBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.network.MatsimLaneDefinitionsWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;

/**
 * @author dgrether
 * 
 */
public class DaganzoScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(DaganzoScenarioGenerator.class);

	private static final String DAGANZOBASE = DgPaths.SHAREDSVN + "studies/dgrether/daganzo/";
	
	private static final String networkFileNew = DAGANZOBASE
			+ "daganzoNetwork.xml";

	public static final String networkFile = networkFileNew;

	private static final String plans1Out = DAGANZOBASE
			+ "daganzoPlansNormalRoute.xml";

	private static final String plans2Out = DAGANZOBASE
			+ "daganzoPlansAlternativeRoute.xml";

	private static final String config1Out = DAGANZOBASE
			+ "daganzoConfigNormalRoute.xml";

	private static final String config2Out = DAGANZOBASE
			+ "daganzoConfigAlternativeRoute.xml";

	public static final String lanesOutputFile = DAGANZOBASE
		+ "daganzoLaneDefinitions.xml";

	public static final String signalSystemsOutputFile = DAGANZOBASE
		+ "daganzoSignalSystems.xml";
	
	public static final String signalSystemConfigurationsOutputFile = DAGANZOBASE 
		+ "daganzoSignalSystemsConfigs.xml";
	
	private static final String outputDirectoryNormalRoute = DAGANZOBASE
		+ "output/normalRoute/";
	
	private static final String outputDirectoryAlternativeRoute = DAGANZOBASE
		+ "output/alternativeRoute/";
	
	
	public static String configOut, plansOut, outputDirectory;

	private static final boolean isAlternativeRouteEnabled = true;

	private static final int iterations = 500;

	private static final int iterations2 = 0;

	private static final String controlerClass = AdaptiveController.class.getCanonicalName();

	private Id id1, id2, id4, id5, id6;
	
	public DaganzoScenarioGenerator() {
		init();
	}

	private void init() {
		if (isAlternativeRouteEnabled) {
			plansOut = plans2Out;
			configOut = config2Out;
			outputDirectory = outputDirectoryAlternativeRoute;
		}
		else {
			plansOut = plans1Out;
			configOut = config1Out;
			outputDirectory = outputDirectoryNormalRoute;
		}
	}
	
	private void createIds(Scenario sc){
		id1 = sc.createId("1");
		id2 = sc.createId("2");
		id4 = sc.createId("4");
		id5 = sc.createId("5");
		id6 = sc.createId("6");
	}

	private void createScenario() {
		//create a config
		Config config = new Config();
		config.addCoreModules();
		//set the network
		config.network().setInputFile(networkFile);
		//create a scenario instance (we have to work on the implemenation not on the interface
		//because some methods are not standardized yet)
		ScenarioImpl scenario = new ScenarioImpl(config);
		//create some ids as members of the class for convenience reasons
		createIds(scenario);
		//create the plans and write them
		createPlans(scenario);
		PopulationWriter pwriter = new PopulationWriter(scenario.getPopulation(), plansOut);
		pwriter.write();
		//create the lanes and write them
		BasicLaneDefinitions lanes = createLanes(scenario);
		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(lanes);
		laneWriter.writeFile(lanesOutputFile);
		//create the signal systems and write them
		BasicSignalSystems signalSystems = createSignalSystems(scenario);
		MatsimSignalSystemsWriter ssWriter = new MatsimSignalSystemsWriter(signalSystems);
		ssWriter.writeFile(signalSystemsOutputFile);
		//create the signal system's configurations and write them
		BasicSignalSystemConfigurations ssConfigs = createSignalSystemsConfig(scenario);
		MatsimSignalSystemConfigurationsWriter ssConfigsWriter = new MatsimSignalSystemConfigurationsWriter(ssConfigs);	
		ssConfigsWriter.writeFile(signalSystemConfigurationsOutputFile);
		
		//create and write the config with the correct paths to the files created above
		createConfig(config);
		ConfigWriter configWriter = new ConfigWriter(config, configOut);
		configWriter.write();

		log.info("scenario written!");
	}


	private void createPlans(Scenario scenario) {
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		int firstHomeEndTime = 0;// 6 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = network.getLink(scenario.createId("1"));
		Link l7 = network.getLink(scenario.createId("7"));
		PopulationBuilder builder = population.getPopulationBuilder();

		for (int i = 1; i <= 3600; i++) {
			Person p = builder.createPerson(scenario.createId(Integer
					.toString(i)));
			Plan plan = builder.createPlan(p);
			p.addPlan(plan);
			// home
			// homeEndTime = homeEndTime + ((i - 1) % 3);
			if ((i - 1) % 3 == 0) {
				homeEndTime++;
			}

			Activity act1 = builder.createActivityFromLinkId("h", l1.getId());
			act1.setEndTime(homeEndTime);
			plan.addActivity(act1);
			// leg to home
			Leg leg = builder.createLeg(TransportMode.car);
			// TODO check this
			NetworkRoute route = new NodeNetworkRoute(l1, l7);
			if (isAlternativeRouteEnabled) {
				route
						.setNodes(l1, NetworkUtils.getNodes(network, "2 3 4 5 6"), l7);
			}
			else {
				route.setNodes(l1, NetworkUtils.getNodes(network, "2 3 5 6"), l7);
			}
			leg.setRoute(route);

			Activity act2 = builder.createActivityFromLinkId("h", l7.getId());
			act2.setLink(l7);
			population.addPerson(p);
		}
	}
	
	private void createConfig(Config config) {
	// set scenario
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(plansOut);
		config.network().setLaneDefinitionsFile(lanesOutputFile);
		config.signalSystems().setSignalSystemFile(signalSystemsOutputFile);
		config.signalSystems().setSignalSystemConfigFile(signalSystemConfigurationsOutputFile);
		
		// configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(6.0);
		// this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
		// homeParams.setOpeningTime(0);
		config.charyparNagelScoring().addActivityParams(homeParams);
		// set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0",
				"24:00:00");

		// configure controler
		config.travelTimeCalculator().setTraveltimeBinSize(1);
		config.controler().setLastIteration(iterations + iterations2);
		config.controler().setOutputDirectory(outputDirectory);

		
		// configure simulation and snapshot writing
		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotFile("cmcf.mvi");
		config.simulation().setSnapshotPeriod(60.0);
		// configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(1));
		selectExp.setProbability(0.9);
		selectExp.setModuleName("ChangeExpBeta");
		config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(2));
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		config.strategy().addStrategySettings(reRoute);
	}
	

	private BasicLaneDefinitions createLanes(ScenarioImpl scenario) {
		BasicLaneDefinitions lanes = scenario.getLaneDefinitions();
		BasicLaneDefinitionsBuilder builder = lanes.getLaneDefinitionBuilder();
		//lanes for link 4
		BasicLanesToLinkAssignment lanesForLink4 = builder.createLanesToLinkAssignment(id4);
		BasicLane link4lane1 = builder.createLane(id1);
		link4lane1.addToLinkId(id6);
		lanesForLink4.addLane(link4lane1);
		lanes.addLanesToLinkAssignment(lanesForLink4);
		//lanes for link 5
		BasicLanesToLinkAssignment lanesForLink5 = builder.createLanesToLinkAssignment(id5);
		BasicLane link5lane1 = builder.createLane(id1);
		link5lane1.addToLinkId(id6);
		lanesForLink5.addLane(link5lane1);
		lanes.addLanesToLinkAssignment(lanesForLink5);
		return lanes;
	}

	
	private BasicSignalSystems createSignalSystems(ScenarioImpl scenario) {
		BasicSignalSystems systems = scenario.getSignalSystems();
		BasicSignalSystemsBuilder builder = systems.getSignalSystemsBuilder();
		//create the signal system
		BasicSignalSystemDefinition definition = builder.createSignalSystemDefinition(id1);
		systems.addSignalSystemDefinition(definition);
		
		//create signal group for traffic on link 4 on lane 1 with toLink 6
		BasicSignalGroupDefinition groupLink4 = builder.createSignalGroupDefinition(id4, id1);
		groupLink4.addLaneId(id1);
		groupLink4.addToLinkId(id6);
		systems.addSignalGroupDefinition(groupLink4);
		
		//create signal group for traffic on link 5 on lane 1 with toLink 6
		BasicSignalGroupDefinition groupLink5 = builder.createSignalGroupDefinition(id5, id2);
		groupLink5.addLaneId(id1);
		groupLink5.addToLinkId(id6);
		systems.addSignalGroupDefinition(groupLink5);
		
		return systems;
	}

	private BasicSignalSystemConfigurations createSignalSystemsConfig(
			ScenarioImpl scenario) {
		BasicSignalSystemConfigurations configs = scenario.getSignalSystemsConfiguration();
		BasicSignalSystemConfigurationsBuilder builder = configs.getBuilder();
		
		BasicSignalSystemConfiguration systemConfig = builder.createSignalSystemConfiguration(id1);
		BasicAdaptiveSignalSystemControlInfo controlInfo = builder.createAdaptiveSignalSystemControlInfo();
		controlInfo.addSignalGroupId(id1);
		controlInfo.addSignalGroupId(id2);
		controlInfo.setAdaptiveControlerClass(controlerClass);
		systemConfig.setSignalSystemControlInfo(controlInfo);
		
		configs.getSignalSystemConfigurations().put(systemConfig.getSignalSystemId(), systemConfig);
		
		return configs;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new DaganzoScenarioGenerator().createScenario();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
