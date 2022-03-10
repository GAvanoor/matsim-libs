package lspShipmentTest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import lsp.shipment.LSPShipmentImpl;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import lsp.shipment.LSPShipment;

public class CollectionShipmentBuilderTest {

	private Network network;
	private ArrayList<LSPShipment> shipments;
	
	
	@Before
	public void initialize(){
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();
        ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());
        Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Link> toLinkId = network.getLinks().get(collectionLinkId).getId();
        this.shipments = new ArrayList<>();
        
        for(int i = 1; i < 11; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	LSPShipmentImpl.LSPShipmentBuilder builder = LSPShipmentImpl.LSPShipmentBuilder.newInstance(id );
        	int capacityDemand = new Random().nextInt(10);
        	builder.setCapacityDemand(capacityDemand);
        	
        	while(true) {
        		Collections.shuffle(linkList);
        		Link pendingFromLink = linkList.get(0);
        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
        		   builder.setFromLinkId(pendingFromLink.getId());
        		   break;	
        		}
        	
        	}
        	
        	builder.setToLinkId(toLinkId);
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setDeliveryServiceTime(capacityDemand * 60 );
        	shipments.add(builder.build());
        }
	}
	
	@Test
	public void testShipments() {
		assertEquals(10, shipments.size());
		for(LSPShipment shipment : shipments) {
			assertNotNull(shipment.getId());
			assertNotNull(shipment.getSize() );
			assertNotNull(shipment.getDeliveryTimeWindow() );
			assertNotNull(shipment.getFrom() );
			assertNotNull(shipment.getDeliveryServiceTime() );
			assertNotNull(shipment.getTo() );
			assertNotNull(shipment.getPickupTimeWindow() );
			assertNotNull(shipment.getShipmentPlan());
			assertNotNull(shipment.getLog());
			assertNotNull(shipment.getEventHandlers());
			
			assertTrue(shipment.getEventHandlers().isEmpty());
			assertEquals(shipment.getLog().getShipment(), shipment);
			assertTrue(shipment.getLog().getPlanElements().isEmpty());
			
			assertEquals(shipment.getShipmentPlan().getShipment(), shipment);
			assertTrue(shipment.getShipmentPlan().getPlanElements().isEmpty());
			
			Link link = network.getLinks().get(shipment.getFrom() );
			assertTrue(link.getFromNode().getCoord().getX() <= 4000);
			assertTrue(link.getFromNode().getCoord().getY() <= 4000);

		}
	}
	
}
