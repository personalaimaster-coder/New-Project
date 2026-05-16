package com.example.petmeds.ui.play

/** Number of vertical lanes in the playfield. Lane indices are `0..LANE_COUNT - 1`. */
const val LANE_COUNT = 3

enum class GamePhase { IDLE, PLAYING, GAME_OVER }

enum class ItemKind { TREAT, POISON }

enum class Swipe { LEFT, RIGHT }

/**
 * A single item on the playfield. `yFraction` is the vertical position in
 * `[0f, 1f]` where 0 is the top of the playfield and ~1 is the puppy row.
 */
data class FallingItem(
    val id: Long,
    val lane: Int,
    val yFraction: Float,
    val kind: ItemKind,
    /** Selects which sprite variant the renderer uses within a kind. Wrapped mod variant-count. */
    val variantIndex: Int = 0,
)

/**
 * Full game state. Pure data — held in the ViewModel and rendered by the
 * Compose layer. `speed` is fraction-per-second of yFraction the items
 * advance. `spawnAccumulator` tracks elapsed time since the last spawn so
 * the spawn cadence stays decoupled from the frame rate.
 */
data class GameState(
    val phase: GamePhase,
    val puppyLane: Int,
    val items: List<FallingItem>,
    val score: Int,
    val highScore: Int,
    val previousHighScore: Int,
    val speed: Float,
    val spawnAccumulator: Long,
    val nextItemId: Long,
)

/** Side-effects the engine wants the ViewModel to fire (audio, haptics, persistence). */
sealed class GameEvent {
    object Started : GameEvent()
    object LaneChanged : GameEvent()
    /** Treat reached the puppy row in the puppy's lane. */
    object Caught : GameEvent()
    /** Poison reached the puppy row in the puppy's lane. Engine has transitioned to GAME_OVER. */
    object Hit : GameEvent()
}

data class TickResult(
    val state: GameState,
    val events: List<GameEvent>,
)
