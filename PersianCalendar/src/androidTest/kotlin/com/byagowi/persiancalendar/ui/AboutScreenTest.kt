package com.byagowi.persiancalendar.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.ui.about.AboutScreen
import com.byagowi.persiancalendar.ui.about.DeviceInformationScreen
import com.byagowi.persiancalendar.ui.about.LicensesScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Have a look at https://developer.android.com/static/images/jetpack/compose/compose-testing-cheatsheet.pdf
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutScreenNavigateToDeviceInformation() {
        var navigateToDeviceInformationIsCalled = false
        var deviceInformationString = ""
        composeTestRule.setContentWithParent { scope ->
            deviceInformationString = stringResource(R.string.device_information)
            AboutScreen(
                animatedContentScope = scope,
                openDrawer = {},
                navigateToDeviceInformation = { navigateToDeviceInformationIsCalled = true },
                navigateToLicenses = { assert(false) },
            )
        }
        composeTestRule.onNodeWithContentDescription(deviceInformationString)
            .assertHasClickAction()
            .performClick()
        assert(navigateToDeviceInformationIsCalled)
    }

    @Test
    fun aboutScreenNavigateToLicenses() {
        var navigateToLicensesIsCalled = false
        var licensesString = ""
        composeTestRule.setContentWithParent { scope ->
            licensesString = stringResource(R.string.about_license_title)
            AboutScreen(
                animatedContentScope = scope,
                openDrawer = {},
                navigateToDeviceInformation = { assert(false) },
                navigateToLicenses = { navigateToLicensesIsCalled = true },
            )
        }
        composeTestRule.onNodeWithText(licensesString)
            .assertHasClickAction()
            .performClick()
        assert(navigateToLicensesIsCalled)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun someImportantTest() {
        var called = false
        var title = ""
        composeTestRule.setContentWithParent { scope ->
            title = stringResource(R.string.about_license_title)
            Scaffold { padding ->
                padding.let {}
                Box(
                    modifier = Modifier.sharedBounds(
                        rememberSharedContentState(key = "SOME_SAMPLE_KEY"),
                        animatedVisibilityScope = scope,
                    )
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(title) } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { called = true }) {
                            Icon(Icons.Default.PermDeviceInformation, contentDescription = title)
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithText(title)
            .assertHasClickAction()
            .performClick()
        assert(called)
    }

    @Test
    fun deviceInformationSmokeTest() {
        composeTestRule.setContentWithParent { scope ->
            DeviceInformationScreen({}, scope)
        }
    }

    @Test
    fun licensesSmokeTest() {
        composeTestRule.setContentWithParent { scope -> LicensesScreen(scope) {} }
    }
}
