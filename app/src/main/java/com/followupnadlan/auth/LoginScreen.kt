package com.followupnadlan.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.accessibility.AccessibilityColors
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import kotlinx.coroutines.launch

private enum class LoginStep { PHONE, CODE }

private val LinkColor = Color(0xFF027EB5)

/**
 * WhatsApp-OTP login gate. Two steps in one screen:
 *  PHONE -> normalize + POST /otp/request; only advance to CODE on a real 200 sent:true (§2).
 *  CODE  -> POST /otp/verify; on success persist the token and call [onAuthenticated].
 * RTL / app theme. All server outcomes surface an honest message; never a false "code sent".
 */
@Composable
fun LoginScreen(
    authTokenStore: AuthTokenStore,
    myDetailsStore: MyDetailsStore,
    onAuthenticated: () -> Unit,
    apiClient: OtpApiClient = remember { OtpApiClient() }
) {
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(LoginStep.PHONE) }
    var phoneInput by remember {
        mutableStateOf(PhoneNumberNormalizer.toLocalIsraeliDisplay(myDetailsStore.load().phone))
    }
    var codeInput by remember { mutableStateOf("") }
    // The normalized 972 number, set once the phone step succeeds; used by verify + saveToken.
    var phone972 by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var infoText by remember { mutableStateOf("") }

    fun onSendCode() {
        errorText = ""
        infoText = ""
        val normalized = PhoneNumberNormalizer.normalizeForWhatsApp(phoneInput)
        if (normalized == null) {
            errorText = "מספר לא תקין"
            return
        }
        loading = true
        scope.launch {
            val result = apiClient.requestCode(normalized)
            loading = false
            when (result) {
                is OtpApiClient.RequestResult.Sent -> {
                    phone972 = normalized
                    codeInput = ""
                    step = LoginStep.CODE
                    infoText = "קוד נשלח לוואטסאפ שלך"
                }
                is OtpApiClient.RequestResult.RateLimited -> errorText = "נסה שוב בעוד דקה"
                is OtpApiClient.RequestResult.SendFailed -> errorText = "לא הצלחנו לשלוח קוד, נסה שוב"
                is OtpApiClient.RequestResult.NetworkError -> errorText = "אין חיבור לאינטרנט"
            }
        }
    }

    fun onVerify() {
        errorText = ""
        loading = true
        scope.launch {
            val result = apiClient.verify(phone972, codeInput.trim())
            loading = false
            when (result) {
                is OtpApiClient.VerifyResult.Success -> {
                    authTokenStore.saveToken(
                        token = result.token,
                        phone = phone972,
                        expiresAtMs = result.expiresAtMs,
                        nowMs = System.currentTimeMillis()
                    )
                    onAuthenticated()
                }
                is OtpApiClient.VerifyResult.Invalid -> errorText = "קוד שגוי"
                is OtpApiClient.VerifyResult.NetworkError -> errorText = "אין חיבור לאינטרנט"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔐", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (step == LoginStep.PHONE) "כניסה לאפליקציה" else "הזן את הקוד",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = AccessibilityColors.Heading,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (step == LoginStep.PHONE)
                    "נשלח לך קוד אימות בוואטסאפ"
                else
                    "הזן את הקוד בן 6 הספרות שקיבלת",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = AccessibilityColors.TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (step == LoginStep.PHONE) {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it; errorText = "" },
                    placeholder = { Text("לדוגמה: 050-1234567", color = AccessibilityColors.TextFaint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            } else {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { new -> codeInput = new.filter { it.isDigit() }.take(6); errorText = "" },
                    placeholder = { Text("123456", color = AccessibilityColors.TextFaint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (infoText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(infoText, fontSize = 14.sp, color = AccessibilityColors.Green, textAlign = TextAlign.Center)
            }
            if (errorText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorText, fontSize = 14.sp, color = AccessibilityColors.Danger, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val enabled = !loading && when (step) {
                LoginStep.PHONE -> phoneInput.isNotBlank()
                LoginStep.CODE -> codeInput.length == 6
            }
            Button(
                onClick = { if (step == LoginStep.PHONE) onSendCode() else onVerify() },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccessibilityColors.Green,
                    contentColor = Color.White
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (step == LoginStep.PHONE) "שלח קוד" else "אמת",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (step == LoginStep.CODE) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "שנה מספר",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LinkColor,
                    modifier = Modifier.clickable(enabled = !loading) {
                        step = LoginStep.PHONE
                        codeInput = ""
                        errorText = ""
                        infoText = ""
                    }
                )
            }
        }
    }
}
