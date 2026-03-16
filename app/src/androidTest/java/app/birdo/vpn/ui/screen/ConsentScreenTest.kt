package app.birdo.vpn.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.birdo.vpn.ui.theme.BirdoTheme
import org.junit.Rule
import org.junit.Test

class ConsentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun consentScreen_displaysPrivacyInfo() {
        composeTestRule.setContent {
            BirdoTheme {
                ConsentScreen(
                    onAccept = {},
                    onDecline = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Your Privacy Matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Activity Logs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Minimal Account Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crash Reports").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Data Sales").assertIsDisplayed()
    }

    @Test
    fun consentScreen_displaysButtons() {
        composeTestRule.setContent {
            BirdoTheme {
                ConsentScreen(
                    onAccept = {},
                    onDecline = {},
                )
            }
        }

        composeTestRule.onNodeWithText("I Agree & Continue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Decline").assertIsDisplayed()
    }

    @Test
    fun consentScreen_acceptCallsCallback() {
        var accepted = false
        composeTestRule.setContent {
            BirdoTheme {
                ConsentScreen(
                    onAccept = { accepted = true },
                    onDecline = {},
                )
            }
        }

        composeTestRule.onNodeWithText("I Agree & Continue").performClick()
        assert(accepted) { "Accept callback should have been called" }
    }

    @Test
    fun consentScreen_declineCallsCallback() {
        var declined = false
        composeTestRule.setContent {
            BirdoTheme {
                ConsentScreen(
                    onAccept = {},
                    onDecline = { declined = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Decline").performClick()
        assert(declined) { "Decline callback should have been called" }
    }
}
