package io.beldex.bchat.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.beldex.bchat.BaseCoroutineTest
import org.junit.Rule

open class BaseViewModelTest: BaseCoroutineTest() {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

}