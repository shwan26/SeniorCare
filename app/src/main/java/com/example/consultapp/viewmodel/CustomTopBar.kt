import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.consultapp.R
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null, // Optional: only show the back icon if provided
    darkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onLanguageChanged: (Locale) -> Unit // Accepts Locale as an argument
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Display the logo image
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .height(40.dp) // Adjust the height as needed
                )
                // Display the title text
                Text(text = title)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                // Provide the back button if onBack is provided
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            // Language toggle button
            IconButton(onClick = { onLanguageChanged(Locale("en")) }) {
                Icon(Icons.Filled.Translate, contentDescription = "Change Language")
            }
            // Dark mode toggle button
            IconButton(onClick = onToggleDarkMode) {
                if (darkModeEnabled) {
                    Icon(Icons.Filled.BrightnessHigh, contentDescription = "Switch to Light Mode")
                } else {
                    Icon(Icons.Filled.Brightness4, contentDescription = "Switch to Dark Mode")
                }
            }
        }
    )
}

fun setLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
        context.createConfigurationContext(config)
    } else {
        config.locale = locale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
