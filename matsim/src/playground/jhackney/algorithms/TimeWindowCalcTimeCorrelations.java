package playground.jhackney.algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.socialnet.SocialNetwork;

public class TimeWindowCalcTimeCorrelations {

	public TimeWindowCalcTimeCorrelations(Hashtable<Facility,ArrayList<TimeWindow>> timeWindowMap, String out2name, String out1name){ 
		// First identify the overlapping Acts and the Persons involved
		Object[] facs = timeWindowMap.keySet().toArray();
		Vector<Double> tbins=new Vector<Double>();
		int numbins=360;
		double binwidth=300.;

		BufferedWriter out2=null;

		try {
			out2 = new BufferedWriter(new FileWriter(out2name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out1=null;
		try {
			out1 = new BufferedWriter(new FileWriter(out1name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int j=0; j<numbins; j++){
			tbins.add(j*binwidth);
		}
		double[][] hist= new double[numbins][10];

		try {
			out2.write("pid\ttype\tnum\ttstartpid\ttendpid\ttdurpid\tdistpid\tnumegonet\ttstartegonet\ttendegonet\ttduregonet\tdistegonet"+"\r\n");
		} catch (IOException e1) {
			//"\t"TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for(int i=0;i<facs.length;i++){
			Object[] visits= timeWindowMap.get(facs[i]).toArray();
			for(int ii=0;ii<visits.length;ii++){

				TimeWindow tw1 = (TimeWindow) visits[ii];
				double ti_egonet=tw1.startTime;
				double tf_egonet=tw1.endTime;
				double tdur_egonet=tf_egonet-ti_egonet;
				int n_egonet=0;
				double dist_egonet=tw1.act.getFacility().calcDistance(((Act)(tw1.person.getSelectedPlan().getActsLegs().get(0))).getFacility().getCenter());

				double ti_act=tw1.startTime;
				double tf_act=tw1.endTime;
				double tdur_act=tf_act-ti_act;
				int n_act=0;
				double dist_act=dist_egonet;

				for (int iii=ii+1;iii<visits.length;iii++){
					TimeWindow tw2 = (TimeWindow) visits[iii];
					double dist_alter=tw2.act.getFacility().calcDistance(((Act)(tw2.person.getSelectedPlan().getActsLegs().get(0))).getFacility().getCenter());

					// Others there who are friends
					if(CompareTimeWindows.overlapTimePlaceTypeFriend(tw1, tw2)){
						n_egonet++;
						ti_egonet=ti_egonet+tw2.startTime;
						tf_egonet=tf_egonet+tw2.endTime;
						tdur_egonet=tdur_egonet+(tw2.endTime-tw2.startTime);
						dist_egonet=dist_egonet+dist_alter;

					}
					// Others there, not necessarily friends
					if(CompareTimeWindows.overlapTimePlaceType(tw1, tw2)){										
						n_act++;
//						ti_act=ti_act+tw1.startTime;
//						tf_act=tf_act+tw1.endTime;
						tdur_act=tdur_act+(tw1.endTime-tw1.startTime);
						dist_act=dist_act+dist_alter;
					}
				}

				ti_egonet=ti_egonet/((double) n_egonet+1);
				tf_egonet=tf_egonet/((double) n_egonet+1);
				tdur_egonet=tdur_egonet/((double) n_egonet+1);
				dist_egonet=dist_egonet/((double) n_egonet+1);

//				ti_act=ti_act/((double) n_act+1);
//				tf_act=tf_act/((double) n_act+1);
				tdur_act=tdur_act/((double) n_act+1);
				dist_act=dist_act/((double) n_act+1);

//				System.out.println(tw1.person.getId()+"\t"+tw1.act.getType()+"\t"+n_act+"\t"+ti_act+"\t"+tf_act+"\t"+tdur_act+"\t"+dist_act+"\t"+n_egonet+"\t"+ti_egonet+"\t"+tf_egonet+"\t"+tdur_egonet+"\t"+dist_egonet);
				try {
					out2.write(tw1.person.getId()+"\t"+tw1.act.getType()+"\t"+n_act+"\t"+ti_act+"\t"+tf_act+"\t"+tdur_act+"\t"+dist_act+"\t"+n_egonet+"\t"+ti_egonet+"\t"+tf_egonet+"\t"+tdur_egonet+"\t"+dist_egonet+"\r\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Agents and egonets spending time between StartTime and EndTime
				double tend=((tf_act+numbins*binwidth)%(numbins*binwidth));
				double tstart=(ti_act+numbins*binwidth)%(numbins*binwidth);

				if(tend>=tstart){
					for(int j= (int) (tstart/binwidth); j< (int) (tend/binwidth); j++){
//						System.out.println(j+" "+(((ti_act+numbins*binwidth)%(numbins*binwidth))/300.));
						if(tw1.act.getType().equals("home")){
							hist[j][0]=hist[j][0]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][1]=hist[j][1]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][2]=hist[j][2]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][3]=hist[j][3]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][4]=hist[j][4]+1;					
						}
					}
				}else if(tstart>tend){
					// Agents and egonets spending time between 0 and EndTime
					for(int j= (int) 0; j< (int) (tend/binwidth); j++){
//						System.out.println(j+" "+(((ti_act+numbins*binwidth)%(numbins*binwidth))/300.));
						if(tw1.act.getType().equals("home")){
							hist[j][0]=hist[j][0]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][1]=hist[j][1]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][2]=hist[j][2]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][3]=hist[j][3]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][4]=hist[j][4]+1;					
						}
					}
					for(int j= (int) (tstart/binwidth); j< (int) numbins-1; j++){
//						System.out.println(j+" "+tstart/300+" "+((numbins*binwidth)/binwidth-1));
						if(tw1.act.getType().equals("home")){
							hist[j][0]=hist[j][0]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][1]=hist[j][1]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][2]=hist[j][2]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][3]=hist[j][3]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][4]=hist[j][4]+1;					
						}
					}
				}
				//new definition of tstart/tend
				tend=((tf_egonet+numbins*binwidth)%(numbins*binwidth));
				tstart=(ti_egonet+numbins*binwidth)%(numbins*binwidth);

					if(tend>=tstart){
					for(int j = (int) (tstart/binwidth); j< (int) (tend/binwidth); j++){
//						System.out.println(j+" "+tend+" "+tstart);
						if(tw1.act.getType().equals("home")){
							hist[j][5]=hist[j][5]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][6]=hist[j][6]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][7]=hist[j][7]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][8]=hist[j][8]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][9]=hist[j][9]+1;					
						}
					}
				}else if(tstart>tend){
					for(int j = (int) 0; j< (int) (tend/binwidth); j++){
						if(tw1.act.getType().equals("home")){
							hist[j][5]=hist[j][5]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][6]=hist[j][6]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][7]=hist[j][7]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][8]=hist[j][8]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][9]=hist[j][9]+1;					
						}
					}
					// Agents and egonets spending time between StartTime and 90000

					for(int j = (int) (tstart/binwidth); j< (int) numbins-1; j++){
						if(tw1.act.getType().equals("home")){
							hist[j][5]=hist[j][5]+1;					
						}
						if(tw1.act.getType().equals("work")){
							hist[j][6]=hist[j][6]+1;					
						}
						if(tw1.act.getType().equals("shop")){
							hist[j][7]=hist[j][7]+1;					
						}
						if(tw1.act.getType().equals("leisure")){
							hist[j][8]=hist[j][8]+1;					
						}
						if(tw1.act.getType().equals("education")){
							hist[j][9]=hist[j][9]+1;					
						}
					}
				}
			}
			//
			//
		}

		try {
			out1.write("time\thome_ego\twork_ego\tshop_ego\tleisure_ego\teducation_ego\thome_net\twork_net\tshop_net\tleisure_net\teducation_net"+"\r\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int j=0;j< numbins;j++){
			try {
				out1.write(tbins.get(j)+"\t"+hist[j][0]+"\t"+hist[j][1]+"\t"+hist[j][2]+"\t"+hist[j][3]+"\t"+hist[j][4]+"\t"+hist[j][5]+"\t"+hist[j][6]+"\t"+hist[j][7]+"\t"+hist[j][8]+"\t"+hist[j][9]+"\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

