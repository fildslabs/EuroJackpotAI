package com.eurojackpotai.app.domain.model

/**
 * All nine modes from the product spec — every one is implemented.
 *
 * [isAvailable] is kept (rather than removed now that nothing is gated) so a future mode
 * can still ship UI-first — greyed out with a "coming soon" badge, see
 * GenerateScreen.ModeRow — before its generation logic exists.
 *
 * AI Mix builds on Statistical Mix with a fuller weighted-scoring engine that blends all
 * three single-factor weights (hot/cold/statistical) and rejects oddly-shaped
 * combinations — see [com.eurojackpotai.app.domain.usecase.GenerateNumbersUseCase.aiMixPick]
 * and [com.eurojackpotai.app.domain.usecase.CombinationHeuristics]. Smart Coverage (the
 * Phase 4 cross-game optimizer) is implemented in
 * [com.eurojackpotai.app.domain.usecase.SmartCoverageGenerator].
 *
 * Avoid Popular Picks biases main numbers away from the 1-31 "calendar day" range (which
 * is over-represented in hand-picked combinations, since it's the only range that can
 * read as a birthday or anniversary) and rejects combinations that still look like a
 * hand-picked, popular set — see
 * [com.eurojackpotai.app.domain.usecase.GenerateNumbersUseCase.avoidPopularPick]. Like
 * every other mode, it does not change any combination's odds of winning; it only
 * affects how likely a jackpot win is to be split with another player who picked the
 * same "intuitive" numbers (see avoid_popular_disclaimer in strings.xml). Hot, Cold and
 * Hot+Cold additionally take a user-adjustable [WeightIntensity] that scales how
 * strongly their statistical bias is applied.
 */
enum class GenerationMode(val isAvailable: Boolean) {
    RANDOM(isAvailable = true),
    BALANCED(isAvailable = true),
    HOT(isAvailable = true),
    COLD(isAvailable = true),
    HOT_COLD(isAvailable = true),
    STATISTICAL_MIX(isAvailable = true),
    AI_MIX(isAvailable = true),
    SMART_COVERAGE(isAvailable = true),
    AVOID_POPULAR(isAvailable = true)
}
