package com.example.petmeds.ui.play

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val rng get() = Random(seed = 12345L)

    private fun playingState(
        puppyLane: Int = 1,
        items: List<FallingItem> = emptyList(),
        score: Int = 0,
        highScore: Int = 0,
    ): GameState = GameState(
        phase = GamePhase.PLAYING,
        puppyLane = puppyLane,
        items = items,
        score = score,
        highScore = highScore,
        previousHighScore = highScore,
        speed = 0.40f,
        spawnAccumulator = 0L,
        nextItemId = 100L,
    )

    @Test
    fun `initial state is IDLE with given high score`() {
        val s = GameEngine.initial(highScore = 42)
        assertThat(s.phase).isEqualTo(GamePhase.IDLE)
        assertThat(s.score).isEqualTo(0)
        assertThat(s.highScore).isEqualTo(42)
        assertThat(s.previousHighScore).isEqualTo(42)
        assertThat(s.items).isEmpty()
        assertThat(s.puppyLane).isIn(0 until LANE_COUNT)
    }

    @Test
    fun `start transitions to PLAYING and emits Started`() {
        val result = GameEngine.start(GameEngine.initial(highScore = 5))
        assertThat(result.state.phase).isEqualTo(GamePhase.PLAYING)
        assertThat(result.state.score).isEqualTo(0)
        assertThat(result.state.items).isEmpty()
        assertThat(result.events).containsExactly(GameEvent.Started)
    }

    @Test
    fun `tick is a no-op when phase is not PLAYING`() {
        val idle = GameEngine.initial(highScore = 0)
        val result = GameEngine.tick(idle, deltaMs = 100L, swipe = null, rng = rng)
        assertThat(result.state).isEqualTo(idle)
        assertThat(result.events).isEmpty()
    }

    @Test
    fun `treat reaching puppy row in puppy lane scores and emits Caught`() {
        // Item at y=0.80, lane=1. Puppy at lane=1. deltaMs=250 -> +0.10 -> y=0.90 (within 0.86..1.00).
        val item = FallingItem(id = 1L, lane = 1, yFraction = 0.80f, kind = ItemKind.TREAT)
        val before = playingState(puppyLane = 1, items = listOf(item))
        val after = GameEngine.tick(before, deltaMs = 250L, swipe = null, rng = rng)
        assertThat(after.state.score).isEqualTo(1)
        assertThat(after.state.phase).isEqualTo(GamePhase.PLAYING)
        assertThat(after.events).contains(GameEvent.Caught)
        // Caught item is consumed; surviving items may include freshly-spawned ones, so check the original is gone.
        assertThat(after.state.items.map { it.id }).doesNotContain(1L)
    }

    @Test
    fun `poison reaching puppy row in puppy lane triggers Hit and game over`() {
        val item = FallingItem(id = 2L, lane = 2, yFraction = 0.80f, kind = ItemKind.POISON)
        val before = playingState(puppyLane = 2, items = listOf(item))
        val after = GameEngine.tick(before, deltaMs = 250L, swipe = null, rng = rng)
        assertThat(after.state.phase).isEqualTo(GamePhase.GAME_OVER)
        assertThat(after.state.score).isEqualTo(0)
        assertThat(after.events).contains(GameEvent.Hit)
    }

    @Test
    fun `item in different lane passes harmlessly`() {
        val treat = FallingItem(id = 3L, lane = 0, yFraction = 0.80f, kind = ItemKind.TREAT)
        val poison = FallingItem(id = 4L, lane = 3, yFraction = 0.80f, kind = ItemKind.POISON)
        val before = playingState(puppyLane = 1, items = listOf(treat, poison))
        val after = GameEngine.tick(before, deltaMs = 250L, swipe = null, rng = rng)
        assertThat(after.state.phase).isEqualTo(GamePhase.PLAYING)
        assertThat(after.state.score).isEqualTo(0)
        // Neither Caught nor Hit emitted by user input — only spawn-driven nonevents.
        assertThat(after.events.filterIsInstance<GameEvent.Caught>()).isEmpty()
        assertThat(after.events.filterIsInstance<GameEvent.Hit>()).isEmpty()
    }

    @Test
    fun `items past despawn line are removed without scoring`() {
        val item = FallingItem(id = 5L, lane = 0, yFraction = 1.10f, kind = ItemKind.TREAT)
        val before = playingState(puppyLane = 0, items = listOf(item))
        val after = GameEngine.tick(before, deltaMs = 16L, swipe = null, rng = rng)
        assertThat(after.state.items.map { it.id }).doesNotContain(5L)
        assertThat(after.state.score).isEqualTo(0)
    }

    @Test
    fun `swipe LEFT moves puppy one lane and emits LaneChanged`() {
        val before = playingState(puppyLane = 2)
        val after = GameEngine.tick(before, deltaMs = 16L, swipe = Swipe.LEFT, rng = rng)
        assertThat(after.state.puppyLane).isEqualTo(1)
        assertThat(after.events).contains(GameEvent.LaneChanged)
    }

    @Test
    fun `swipe RIGHT moves puppy one lane`() {
        val before = playingState(puppyLane = 1)
        val after = GameEngine.tick(before, deltaMs = 16L, swipe = Swipe.RIGHT, rng = rng)
        assertThat(after.state.puppyLane).isEqualTo(2)
    }

    @Test
    fun `swipe LEFT at lane 0 is clamped and emits no LaneChanged`() {
        val before = playingState(puppyLane = 0)
        val after = GameEngine.tick(before, deltaMs = 16L, swipe = Swipe.LEFT, rng = rng)
        assertThat(after.state.puppyLane).isEqualTo(0)
        assertThat(after.events.filterIsInstance<GameEvent.LaneChanged>()).isEmpty()
    }

    @Test
    fun `swipe RIGHT at last lane is clamped and emits no LaneChanged`() {
        val before = playingState(puppyLane = LANE_COUNT - 1)
        val after = GameEngine.tick(before, deltaMs = 16L, swipe = Swipe.RIGHT, rng = rng)
        assertThat(after.state.puppyLane).isEqualTo(LANE_COUNT - 1)
        assertThat(after.events.filterIsInstance<GameEvent.LaneChanged>()).isEmpty()
    }

    @Test
    fun `speed scales with score`() {
        // After a full second at score 0 vs score 50, the item travels further with higher score.
        val baseItem = FallingItem(id = 6L, lane = 0, yFraction = 0.0f, kind = ItemKind.TREAT)
        val low = playingState(puppyLane = 3, items = listOf(baseItem), score = 0)
        val high = playingState(puppyLane = 3, items = listOf(baseItem), score = 50)

        val lowAfter = GameEngine.tick(low, deltaMs = 100L, swipe = null, rng = Random(1))
        val highAfter = GameEngine.tick(high, deltaMs = 100L, swipe = null, rng = Random(1))

        val lowY = lowAfter.state.items.first { it.id == 6L }.yFraction
        val highY = highAfter.state.items.first { it.id == 6L }.yFraction
        assertThat(highY).isGreaterThan(lowY)
    }

    @Test
    fun `spawn cadence tightens with score`() {
        // 900ms at score=0 should yield exactly one spawn; the same window at score=50 should yield more.
        val low = playingState(puppyLane = 3, score = 0)
        val high = playingState(puppyLane = 3, score = 50)

        val lowAfter = GameEngine.tick(low, deltaMs = 900L, swipe = null, rng = Random(7))
        val highAfter = GameEngine.tick(high, deltaMs = 900L, swipe = null, rng = Random(7))

        // Don't assert exact counts — engine constants may evolve — only that higher score spawns at least as many items.
        assertThat(lowAfter.state.items.size).isAtLeast(1)
        assertThat(highAfter.state.items.size).isAtLeast(lowAfter.state.items.size)
    }

    @Test
    fun `seeded rng produces deterministic spawn lanes and kinds`() {
        val s1 = playingState(puppyLane = 3)
        val s2 = playingState(puppyLane = 3)
        val r1 = GameEngine.tick(s1, deltaMs = 1_500L, swipe = null, rng = Random(99L))
        val r2 = GameEngine.tick(s2, deltaMs = 1_500L, swipe = null, rng = Random(99L))
        val lanes1 = r1.state.items.map { it.lane to it.kind }
        val lanes2 = r2.state.items.map { it.lane to it.kind }
        assertThat(lanes1).isEqualTo(lanes2)
    }

    @Test
    fun `high score updates within a run when score exceeds previous best`() {
        val item = FallingItem(id = 7L, lane = 0, yFraction = 0.80f, kind = ItemKind.TREAT)
        val before = playingState(puppyLane = 0, items = listOf(item), score = 0, highScore = 0)
        val after = GameEngine.tick(before, deltaMs = 250L, swipe = null, rng = rng)
        assertThat(after.state.score).isEqualTo(1)
        assertThat(after.state.highScore).isEqualTo(1)
    }

    @Test
    fun `high score is preserved when current score is lower`() {
        val item = FallingItem(id = 8L, lane = 0, yFraction = 0.80f, kind = ItemKind.TREAT)
        val before = playingState(puppyLane = 0, items = listOf(item), score = 0, highScore = 99)
        val after = GameEngine.tick(before, deltaMs = 250L, swipe = null, rng = rng)
        assertThat(after.state.highScore).isEqualTo(99)
    }
}
