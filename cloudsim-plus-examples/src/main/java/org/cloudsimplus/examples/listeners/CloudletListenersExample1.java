package org.cloudsimplus.examples.listeners;

/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */
import org.cloudsimplus.util.tablebuilder.CloudletsTableBuilderHelper;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSimple;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSimple;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.listeners.VmToCloudletEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Ram;
import org.cloudbus.cloudsim.schedulers.CloudletSchedulerSpaceShared;

/**
 * A simple example showing how to create a data center with 1 host and run
 * cloudlets on it, and receive notifications when each cloudlet finishes executing.
 *
 * The example uses the new Cloudlet listeners to get these
 * notifications while the simulation is running.
 *
 * @see Cloudlet#setOnCloudletFinishEventListener(EventListener)
 * @see EventListener
 *
 * @author Manoel Campos da Silva Filho
 */
public class CloudletListenersExample1 {
    /**
     * Number of Processor Elements (CPU Cores) of each Host.
     */
    private static final int HOST_PES_NUMBER = 4;

    /**
     * Number of Processor Elements (CPU Cores) of each VM and cloudlet.
     */
    private static final int VM_PES_NUMBER = HOST_PES_NUMBER;

    /**
     * Number of Cloudlets to create.
     */
    private static final int NUMBER_OF_CLOUDLETS = VM_PES_NUMBER;

    private final List<Host> hostList;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
    private final DatacenterBroker broker;
    private final Datacenter datacenter;
    private final CloudSim simulation;

    /**
     * The listener object that will be created in order to be notified when
     * a cloudlet finishes executing. The same listener is used for all created cloudlets.
     *
     * @see #createCloudletListener()
     */
    private EventListener<VmToCloudletEventInfo> onCloudletFinishListener;

    /**
     * Starts the example execution, calling the class constructor\
     * to build and run the simulation.
     *
     * @param args command line parameters
     */
    public static void main(String[] args) {
        Log.printFormattedLine("Starting %s ...", CloudletListenersExample1.class.getSimpleName());
        new CloudletListenersExample1();
        Log.printFormattedLine("%s finished!", CloudletListenersExample1.class.getSimpleName());
    }

    /**
     * Default constructor that builds and starts the simulation.
     */
    public CloudletListenersExample1() {
        int numberOfUsers = 1; // number of cloud users/customers (brokers)
        simulation = new CloudSim(numberOfUsers);

        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.datacenter = createDatacenter();
        this.broker = new DatacenterBrokerSimple(simulation);

        createCloudletListener();
        createAndSubmitVms();
        createAndSubmitCloudlets(this.vmList.get(0));

        runSimulationAndPrintResults();
    }

    /**
     * Creates the listener object that will be notified when a cloudlet
     * finishes running into a VM. All cloudlet will use this same listener.
     *
     * @see #createCloudlet(int, org.cloudbus.cloudsim.Vm, long)
     */
    private void createCloudletListener() {
        this.onCloudletFinishListener = new EventListener<VmToCloudletEventInfo>() {
            @Override
            public void update(VmToCloudletEventInfo evt) {
                Log.printFormattedLine(
                        "\n\t#EventListener: Cloudlet %d finished running at Vm %d at time %.2f\n",
                        evt.getCloudlet().getId(), evt.getVm().getId(), evt.getTime());
            }
        };
    }

    private void runSimulationAndPrintResults() {
        simulation.start();
        simulation.stop();

        List<Cloudlet> finishedCloudlets = broker.getCloudletsFinishedList();
        new CloudletsTableBuilderHelper(finishedCloudlets).build();
    }

    /**
     * Creates cloudlets and submit them to the broker.
     * @param vm Vm to run the cloudlets to be created
     *
     * @see #createCloudlet(int, Vm, long)
     */
    private void createAndSubmitCloudlets(Vm vm) {
        int cloudletId;
        long length = 10000;
        for(int i = 0; i < NUMBER_OF_CLOUDLETS; i++){
            cloudletId = vm.getId() + i;
            Cloudlet cloudlet = createCloudlet(cloudletId, vm, length*(i+1));
            this.cloudletList.add(cloudlet);
        }

        this.broker.submitCloudletList(cloudletList);
    }

    /**
     * Creates VMs and submit them to the broker.
     */
    private void createAndSubmitVms() {
        Vm vm0 = createVm(0);
        this.vmList.add(vm0);
        this.broker.submitVmList(vmList);
    }

    /**
     * Creates a VM with pre-defined configuration.
     *
     * @param id the VM id
     * @return the created VM
     */
    private Vm createVm(int id) {
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        long bw = 1000;
        Vm vm = new VmSimple(id, mips, VM_PES_NUMBER)
            .setRam(ram).setBw(bw).setSize(size)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared())
            .setBroker(broker);

        return vm;
    }

    /**
     * Creates a cloudlet with pre-defined configuration.
     *
     * @param id Cloudlet id
     * @param vm vm to run the cloudlet
     * @param length the cloudlet length in number of Million Instructions (MI)
     * @return the created cloudlet
     */
    private Cloudlet createCloudlet(int id, Vm vm, long length) {
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet
            = new CloudletSimple(id, length, pesNumber)
                    .setCloudletFileSize(fileSize)
                    .setCloudletOutputSize(outputSize)
                    .setUtilizationModel(utilizationModel)
                    .setBroker(broker)
                    .setVmId(vm.getId())
                    .setOnCloudletFinishEventListener(onCloudletFinishListener);

        return cloudlet;
    }

    /**
     * Creates a datacenter with pre-defined configuration.
     *
     * @return the created datacenter
     */
    private Datacenter createDatacenter() {
        Host host = createHost(0);
        hostList.add(host);

        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this datacenter
        double costPerBw = 0.0; // the cost of using bw in this resource

        DatacenterCharacteristics characteristics =
            new DatacenterCharacteristicsSimple(hostList)
                .setCostPerSecond(cost)
                .setCostPerMem(costPerMem)
                .setCostPerStorage(costPerStorage)
                .setCostPerBw(costPerBw);

        return new DatacenterSimple(simulation, characteristics, new VmAllocationPolicySimple());
    }

    /**
     * Creates a host with pre-defined configuration.
     *
     * @param id The Host id
     * @return the created host
     */
    private Host createHost(int id) {
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        for(int i = 0; i < HOST_PES_NUMBER; i++){
            peList.add(new PeSimple(i, new PeProvisionerSimple(mips)));
        }
        long ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage (MB)
        long bw = 10000; //Megabits/s

        return new HostSimple(id, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple(new Ram(ram)))
            .setBwProvisioner(new ResourceProvisionerSimple(new Bandwidth(bw)))
            .setVmScheduler(new VmSchedulerTimeShared());

    }
}
