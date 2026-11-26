package com.arequiparescue.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LogicalClock y LogicalTime — Invariantes de tiempo lógico monotónico (DEC-019, RN-RUT-009)")
class LogicalClockTest {

    @Test
    @DisplayName("Inicializa el reloj lógico en t = 0 por defecto")
    void shouldStartAtZeroByDefault() {
        LogicalClock clock = new LogicalClock();

        assertThat(clock.currentTime()).isEqualTo(LogicalTime.ZERO);
        assertThat(clock.currentTicks()).isZero();
    }

    @Test
    @DisplayName("Rechaza la creación de un instante lógico negativo (t < 0)")
    void shouldRejectNegativeLogicalTime() {
        assertThatThrownBy(() -> LogicalTime.of(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("t >= 0");
    }

    @Test
    @DisplayName("Permite avanzar el reloj lógico de forma monotónica no decreciente (t_nuevo >= t_actual)")
    void shouldAdvanceMonotonically() {
        LogicalClock clock = new LogicalClock();

        clock.advanceTo(LogicalTime.of(0L));
        assertThat(clock.currentTicks()).isZero();

        clock.advanceTo(LogicalTime.of(5L));
        assertThat(clock.currentTicks()).isEqualTo(5L);

        clock.advanceTo(LogicalTime.of(5L));
        assertThat(clock.currentTicks()).isEqualTo(5L);

        clock.advanceBy(7L);
        assertThat(clock.currentTicks()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Rechaza retroceder el reloj lógico a un instante anterior (t_nuevo < t_actual)")
    void shouldRejectBackwardTimeTransition() {
        LogicalClock clock = new LogicalClock(LogicalTime.of(10L));

        assertThatThrownBy(() -> clock.advanceTo(LogicalTime.of(9L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Violación de monotonicidad");

        assertThat(clock.currentTicks()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Rechaza incrementos negativos en advanceBy")
    void shouldRejectNegativeDelta() {
        LogicalClock clock = new LogicalClock(LogicalTime.of(4L));

        assertThatThrownBy(() -> clock.advanceBy(-2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delta >= 0");
    }

    @Test
    @DisplayName("synchronizeAtLeast avanza monotónicamente y nunca retrocede cuando recibe un tiempo menor")
    void shouldSynchronizeAtLeastWithoutMovingBackward() {
        LogicalClock clock = new LogicalClock(LogicalTime.ZERO);

        assertThat(clock.synchronizeAtLeast(LogicalTime.of(5L))).isEqualTo(LogicalTime.of(5L));
        assertThat(clock.synchronizeAtLeast(LogicalTime.of(3L))).isEqualTo(LogicalTime.of(5L));
        assertThat(clock.synchronizeAtLeast(LogicalTime.of(5L))).isEqualTo(LogicalTime.of(5L));
        assertThat(clock.synchronizeAtLeast(LogicalTime.of(9L))).isEqualTo(LogicalTime.of(9L));
        assertThat(clock.currentTicks()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Soporta lectura y avance concurrente sin perder actualizaciones ni retroceder")
    void shouldHandleConcurrentReadsAndSynchronizationsSafely() throws Exception {
        LogicalClock clock = new LogicalClock(LogicalTime.ZERO);
        int threads = 8;
        int iterations = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 1; i <= threads; i++) {
                final int worker = i;
                Callable<Long> task = () -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    long lastObserved = 0L;
                    for (int step = 1; step <= iterations; step++) {
                        long target = (long) worker * step;
                        long updated = clock.synchronizeAtLeast(LogicalTime.of(target)).ticks();
                        assertThat(updated).isGreaterThanOrEqualTo(lastObserved);
                        lastObserved = updated;
                        assertThat(clock.currentTicks()).isGreaterThanOrEqualTo(lastObserved);
                    }
                    return lastObserved;
                };
                futures.add(pool.submit(task));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Long> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            assertThat(clock.currentTicks()).isEqualTo((long) threads * iterations);
        } finally {
            pool.shutdownNow();
        }
    }
}
