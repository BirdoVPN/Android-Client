package app.birdo.vpn.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.birdo.vpn.ui.theme.BirdoTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            BirdoTheme {
                LoginScreen(
                    isLoading = false,
                    error = null,
                    onLogin = { _, _ -> },
                    onClearError = {},
                    onSignUp = {},
                )
            }
        }

        // Verify key elements are visible
        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Initialize Uplink").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsLoadingState() {
        composeTestRule.setContent {
            BirdoTheme {
                LoginScreen(
                    isLoading = true,
                    error = null,
                    onLogin = { _, _ -> },
                    onClearError = {},
                    onSignUp = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsError() {
        composeTestRule.setContent {
            BirdoTheme {
                LoginScreen(
                    isLoading = false,
                    error = "Invalid email or password",
                    onLogin = { _, _ -> },
                    onClearError = {},
                    onSignUp = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Invalid email or password").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emailInputWorks() {
        composeTestRule.setContent {
            BirdoTheme {
                LoginScreen(
                    isLoading = false,
                    error = null,
                    onLogin = { _, _ -> },
                    onClearError = {},
                    onSignUp = {},
                )
            }
        }

        composeTestRule.onNodeWithText("you@example.com")
            .performTextInput("user@test.com")
    }
}
