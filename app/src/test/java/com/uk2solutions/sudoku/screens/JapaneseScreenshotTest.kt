package com.uk2solutions.sudoku.screens

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 스토어 홍보 이미지용 원본 화면(Japanese). 결과는 docs/store/src-ja 에 저장된다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "ja-w390dp-h844dp-xxhdpi")
class JapaneseScreenshotTest : ScreenshotTest() {
    override val price: String get() = "¥500"
    override fun outPath() = "../docs/store/src-ja"
}
