package com.example.petmeds.ui.play

import kotlin.math.min
import kotlin.random.Random

/**
 * Pure-Kotlin game engine for Treat Catcher. All state transitions live in
 * `tick`; the Compose layer renders the resulting [GameState] and forwards
 * frame deltas plus optional swipe inputs.
 *
 * Coordinates: `yFraction` is `0f` at the top of the playfield and `1f` at
 * the puppy row. The puppy occupies a horizontal lane band at `y ≈ 1f`.
 */
object GameEngine {

    /** Vertical band (in yFraction) where collisions are tested. */
    private const val PUPPY_ROW_TOP = 0.86f
    private const val PUPPY_ROW_BOTTOM = 1.00f

    /** Items below this y are despawned. */
    private const val DESPAWN_Y = 1.05f

    /** Fall speed in yFraction-per-second at score 0, plus per-point bonus, capped.
     *  Starts leisurely (~1 item crossing in ~4 s) and reaches max only around score 60. */
    private const val BASE_SPEED = 0.22f
    private const val SPEED_PER_POINT = 0.008f
    private const val MAX_SPEED = 1.10f

    /** Spawn cadence in milliseconds — items start sparse and tighten slowly.
     *  At score 0 one item every ~1.8 s; at score 30+ they arrive ~every 0.5 s. */
    private const val BASE_SPAWN_MS = 1_800L
    private const val MIN_SPAWN_MS = 500L
    private const val SPAWN_TIGHTEN_PER_POINT = 22L

    /** Poison probability ramp — no poison at all for the first few catches. */
    private const val BASE_POISON_PROB = 0.00f
    private const val POISON_PROB_PER_POINT = 0.007f
    private const val MAX_POISON_PROB = 0.40f

    fun initial(highScore: Int): GameState = GameState(
        phase = GamePhase.IDLE,
        puppyLane = LANE_COUNT / 2,
        items = emptyList(),
        score = 0,
        highScore = highScore,
        previousHighScore = highScore,
        speed = BASE_SPEED,
        spawnAccumulator = 0L,
        nextItemId = 1L,
    )

    /** Begin a new run, preserving the persisted high score so the HUD can keep showing it. */
    fun start(state: GameState): TickResult = TickResult(
        state = state.copy(
            phase = GamePhase.PLAYING,
            puppyLane = LANE_COUNT / 2,
            items = emptyList(),
            score = 0,
            previousHighScore = state.highScore,
            speed = BASE_SPEED,
            spawnAccumulator = 0L,
            nextItemId = state.nextItemId,
        ),
        events = listOf(GameEvent.Started),
    )

    /**
     * Advance the simulation by `deltaMs`, applying an optional `swipe` input
     * at the start of the frame. No-op when the phase is not PLAYING.
     */
    fun tick(
        state: GameState,
        deltaMs: Long,
        swipe: Swipe?,
        rng: Random,
    ): TickResult {
        if (state.phase != GamePhase.PLAYING) {
            return TickResult(state, emptyList())
        }
        if (deltaMs <= 0L) {
            return TickResult(state, emptyList())
        }
        val events = mutableListOf<GameEvent>()

        // 1) Apply swipe (clamped). One lane per call.
        var puppyLane = state.puppyLane
        if (swipe != null) {
            val target = when (swipe) {
                Swipe.LEFT -> puppyLane - 1
                Swipe.RIGHT -> puppyLane + 1
            }.coerceIn(0, LANE_COUNT - 1)
            if (target != puppyLane) {
                puppyLane = target
                events += GameEvent.LaneChanged
            }
        }

        // 2) Compute current speed + spawn cadence from score.
        val speed = (BASE_SPEED + SPEED_PER_POINT * state.score).coerceAtMost(MAX_SPEED)
        val spawnEveryMs = (BASE_SPAWN_MS - SPAWN_TIGHTEN_PER_POINT * state.score)
            .coerceAtLeast(MIN_SPAWN_MS)

        // 3) Advance existing items.
        val dt = deltaMs / 1000f
        val advanced = state.items.map { it.copy(yFraction = it.yFraction + speed * dt) }

        // 4) Resolve collisions / off-screen items. The puppy row check uses
        //    the LEADING edge entering the band, with an "already-collided"
        //    guard so each item only triggers once.
        var score = state.score
        var phase = state.phase
        val survivors = mutableListOf<FallingItem>()
        for (item in advanced) {
            val previousY = item.yFraction - speed * dt
            val crossingPuppyRow = previousY < PUPPY_ROW_TOP && item.yFraction >= PUPPY_ROW_TOP
            val withinPuppyRow = item.yFraction in PUPPY_ROW_TOP..PUPPY_ROW_BOTTOM
            val collidedNow = crossingPuppyRow && withinPuppyRow && item.lane == puppyLane

            when {
                collidedNow && item.kind == ItemKind.TREAT -> {
                    score += 1
                    events += GameEvent.Caught
                    // consumed — drop from list
                }
                collidedNow && item.kind == ItemKind.POISON -> {
                    phase = GamePhase.GAME_OVER
                    events += GameEvent.Hit
                    // consumed — drop from list
                }
                item.yFraction >= DESPAWN_Y -> Unit
                else -> survivors += item
            }
        }

        // 5) Spawn new items (only while still playing).
        var spawnAcc = state.spawnAccumulator + deltaMs
        var nextId = state.nextItemId
        if (phase == GamePhase.PLAYING) {
            while (spawnAcc >= spawnEveryMs) {
                spawnAcc -= spawnEveryMs
                val poisonProb = (BASE_POISON_PROB + POISON_PROB_PER_POINT * score)
                    .coerceAtMost(MAX_POISON_PROB)
                val kind = if (rng.nextFloat() < poisonProb) ItemKind.POISON else ItemKind.TREAT
                val lane = rng.nextInt(LANE_COUNT)
                survivors += FallingItem(
                    id = nextId,
                    lane = lane,
                    yFraction = -0.05f,
                    kind = kind,
                    variantIndex = rng.nextInt(16),
                )
                nextId += 1
            }
        } else {
            // Game ended this tick — discard pending spawn time.
            spawnAcc = 0L
        }

        val highScore = if (score > state.highScore) score else state.highScore

        return TickResult(
            state = state.copy(
                phase = phase,
                puppyLane = puppyLane,
                items = survivors,
                score = score,
                highScore = highScore,
                speed = speed,
                spawnAccumulator = spawnAcc,
                nextItemId = nextId,
            ),
            events = events,
        )
    }

    /** Smallest deltaMs the engine guarantees stable behaviour for. Exposed for tests. */
    @Suppress("unused")
    fun safeDeltaMs(rawDeltaMs: Long): Long = min(rawDeltaMs, 100L)
}
