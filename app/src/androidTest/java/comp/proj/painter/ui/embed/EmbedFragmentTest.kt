package comp.proj.painter.ui.embed

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import comp.proj.painter.R
import junit.framework.TestCase
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmbedFragmentTest : TestCase(){

    private lateinit var scenario: FragmentScenario<EmbedFragment>

    @Before
    fun setup() {
       // scenario = launchFragmentInContainer(themeResId = R.style.Theme_SpendTracker)
        scenario.moveToState(Lifecycle.State.STARTED)
    }

    @Test
    fun testAddingSpend() {

        val scenario = launchFragment<EmbedFragment>()
    }


}