package edu.iis.mto.testreactor.dishwasher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishWasherTest {

    @Mock
    private WaterPump waterPump;
    @Mock
    private Engine engine;
    @Mock
    private DirtFilter dirtFilter;
    @Mock
    private Door door;

    private DishWasher dishWasher;
    private ProgramConfiguration defaultProgramWithoutTabletsUsed;
    private ProgramConfiguration defaultProgramWithTabletsUsed;
    @BeforeEach
    void setUp(){
        dishWasher=new DishWasher(waterPump,engine,dirtFilter,door);
        defaultProgramWithoutTabletsUsed =ProgramConfiguration.builder()
                .withProgram(WashingProgram.INTENSIVE)
                .withFillLevel(FillLevel.HALF)
                .withTabletsUsed(false)
                .build();
        defaultProgramWithTabletsUsed =ProgramConfiguration.builder()
                .withProgram(WashingProgram.INTENSIVE)
                .withFillLevel(FillLevel.HALF)
                .withTabletsUsed(true)
                .build();
    }

    @Test
    void dishWashingWithDefaultProgramWithoutTabletsUsed() {
        when(!door.closed()).thenReturn(true);
        RunResult result = dishWasher.start(defaultProgramWithoutTabletsUsed);
        assertEquals(success(defaultProgramWithoutTabletsUsed).getStatus(), result.getStatus());
        assertEquals(success(defaultProgramWithoutTabletsUsed).getRunMinutes(),result.getRunMinutes());
    }

    @Test
    void dishWashingWithDefaultProgramWithTabletsUsed() {
        when(!door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(100d);
        RunResult result = dishWasher.start(defaultProgramWithTabletsUsed);
        assertEquals(success(defaultProgramWithoutTabletsUsed).getStatus(), result.getStatus());
        assertEquals(success(defaultProgramWithoutTabletsUsed).getRunMinutes(),result.getRunMinutes());
    }

    @Test
    void dishWasherDoorIsOpen(){
        RunResult result = dishWasher.start(defaultProgramWithoutTabletsUsed);
        assertEquals(error(Status.DOOR_OPEN).getStatus(), result.getStatus());
    }

    @Test
    void dishWasherFilterIsNotClean(){
        when(!door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(20d);
        RunResult result = dishWasher.start(defaultProgramWithTabletsUsed);
        assertEquals(error(Status.ERROR_FILTER).getStatus(), result.getStatus());
    }

    @Test
    void dishWasherEngineIsBroken() throws EngineException {
        when(!door.closed()).thenReturn(true);
        doThrow(EngineException.class).when(engine).runProgram(anyList());
        RunResult result = dishWasher.start(defaultProgramWithoutTabletsUsed);
        assertEquals(error(Status.ERROR_PROGRAM).getStatus(), result.getStatus());
    }

    @Test
    void dishWasherPumpIsBroken() throws PumpException {
        when(!door.closed()).thenReturn(true);
        doThrow(PumpException.class).when(waterPump).drain();
        RunResult result = dishWasher.start(defaultProgramWithoutTabletsUsed);
        assertEquals(error(Status.ERROR_PUMP).getStatus(), result.getStatus());
    }

    @Test
    void dishWashingWithDefaultProgramShouldCallWaterPumpAndEngineMethodsInCorrectOrder() throws PumpException, EngineException {
        when(!door.closed()).thenReturn(true);
        dishWasher.start(defaultProgramWithoutTabletsUsed);

        InOrder inOrder = Mockito.inOrder(engine,waterPump);
        inOrder.verify(waterPump).pour(any());
        inOrder.verify(engine).runProgram(anyList());
        inOrder.verify(waterPump).drain();
    }

    @Test
    void dishWashingWithDefaultProgramShouldCallDoorMethodInCorrectOrder() throws PumpException, EngineException {
        when(!door.closed()).thenReturn(true);
        dishWasher.start(defaultProgramWithoutTabletsUsed);

        InOrder inOrder = Mockito.inOrder(door);
        inOrder.verify(door).closed();
        inOrder.verify(door).lock();
        inOrder.verify(door).unlock();
    }

    @Test
    void dishWashingWithDefaultProgramShouldCallDoorEngineWaterpumpAndDirtFilterMethodsInCorrectOrder() throws PumpException, EngineException {
        when(!door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(100d);
        dishWasher.start(defaultProgramWithTabletsUsed);

        InOrder inOrder = Mockito.inOrder(door,waterPump,engine,dirtFilter);
        inOrder.verify(door).closed();
        inOrder.verify(dirtFilter).capacity();
        inOrder.verify(door).lock();
        inOrder.verify(waterPump).pour(any());
        inOrder.verify(engine).runProgram(anyList());
        inOrder.verify(waterPump).drain();
        inOrder.verify(door).unlock();
    }



    private RunResult error(Status status) {
        return RunResult.builder().withStatus(status).build();
    }

    private RunResult success(ProgramConfiguration properProgram) {
        return RunResult.builder()
                .withStatus(Status.SUCCESS)
                .withRunMinutes(properProgram.getProgram().getTimeInMinutes())
                .build();
    }

}
