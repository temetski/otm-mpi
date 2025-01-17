package otm;

import dispatch.Dispatcher;
import dispatch.EventFluidFluxUpdate;
import dispatch.EventFluidStateUpdate;
import dispatch.EventStopSimulation;
import error.OTMException;
import models.AbstractFluidModel;
import models.AbstractModel;
import mpi.MPIException;
import runner.RunParameters;
import runner.Scenario;
import runner.Timer;
import translator.Translator;

public class OTMRunner {

    public static double run(Scenario scenario, RunParameters runParams, Translator translator, mpi.GraphComm comm) throws OTMException, MPIException {
        Timer comm_timer = new Timer(true);
        Dispatcher dispatcher = new Dispatcher(runParams.start_time);
        scenario.initialize(dispatcher,runParams);
        run_mpi(scenario,runParams.duration,dispatcher,translator,comm,comm_timer);
        scenario.is_initialized = false;
        return comm_timer.get_total_time();
    }

    public static void run(Scenario scenario, RunParameters runParams) throws OTMException {
        Dispatcher dispatcher = new Dispatcher(runParams.start_time);
        scenario.initialize(dispatcher,runParams);
        run_non_mpi(scenario,runParams.duration,dispatcher);
        scenario.is_initialized = false;
    }

    private static void run_mpi(Scenario scenario, float duration, Dispatcher dispatcher, Translator translator, mpi.GraphComm comm, Timer comm_timer) throws OTMException {

        if(scenario.network.models.isEmpty())
            throw new OTMException("No models!");

        if(scenario.network.models.size()!=1)
            throw new OTMException("This currently works only for a single model.");

        AbstractModel abs_model = scenario.network.models.values().iterator().next();

        if(!(abs_model instanceof AbstractFluidModel))
            throw new OTMException("Not a fluid model.");

        dispatcher.set_continue_simulation(true);

        float now = dispatcher.current_time;
        AbstractFluidModel model = (AbstractFluidModel) abs_model;

        // register stop the simulation
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scenario,dispatcher,now+duration));

        // register first models.ctm clock tick
        if(!scenario.network.models.isEmpty()) {
            dispatcher.register_event(new EventMacroFlowUpdateMPI(dispatcher,now + model.dt,model,translator,comm,comm_timer));
            dispatcher.register_event(new EventFluidStateUpdate(dispatcher, now + model.dt, model));
        }

        // process all events
        dispatcher.dispatch_events_to_stop();

    }

    private static void run_non_mpi(Scenario scenario, float duration, Dispatcher dispatcher) throws OTMException {

        if(scenario.network.models.isEmpty())
            throw new OTMException("No models!");

        if(scenario.network.models.size()!=1)
            throw new OTMException("This currently works only for a single model.");

        AbstractModel abs_model = scenario.network.models.values().iterator().next();

        if(!(abs_model instanceof AbstractFluidModel))
            throw new OTMException("Not a fluid model.");

        dispatcher.set_continue_simulation(true);

        float now = dispatcher.current_time;
        AbstractFluidModel model = (AbstractFluidModel) abs_model;

        // register stop the simulation
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scenario,dispatcher,now+duration));

        // register first models.ctm clock tick
        if(!scenario.network.models.isEmpty()) {
            dispatcher.register_event(new EventFluidFluxUpdate(dispatcher, now + model.dt, model));
            dispatcher.register_event(new EventFluidStateUpdate(dispatcher, now + model.dt, model));
        }

        // process all events
        dispatcher.dispatch_events_to_stop();

    }


}
