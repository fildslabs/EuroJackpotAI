package com.eurojackpotai.app.domain.model

/**
 * User-adjustable strength for the Hot / Cold / Hot+Cold generation modes' statistical
 * bias in [com.eurojackpotai.app.domain.usecase.GenerateNumbersUseCase]. This only
 * changes *how strongly* historical draw frequency tilts the weighted sampling — it
 * never changes any combination's actual odds of winning (see hot_disclaimer /
 * cold_disclaimer in strings.xml). [NORMAL] reproduces the original fixed weighting
 * exactly, so existing behavior is unchanged unless the user moves the control.
 */
enum class WeightIntensity(val multiplier: Double) {
    WEAK(0.5),
    NORMAL(1.0),
    STRONG(2.0)
}
