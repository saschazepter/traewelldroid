package de.hbch.traewelling.ui.settings

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jcloquell.androidsecurestorage.SecureStorage
import de.c1710.filemojicompat_ui.views.picker.EmojiPackItemAdapter
import de.hbch.traewelling.R
import de.hbch.traewelling.api.models.status.TagType
import de.hbch.traewelling.shared.LoggedInUserViewModel
import de.hbch.traewelling.shared.SettingsViewModel
import de.hbch.traewelling.shared.SharedValues
import de.hbch.traewelling.theme.LocalColorScheme
import de.hbch.traewelling.theme.LocalFont
import de.hbch.traewelling.theme.MainTheme
import de.hbch.traewelling.ui.composables.ButtonWithIconAndText
import de.hbch.traewelling.ui.composables.ContentDialog
import de.hbch.traewelling.ui.composables.OpenRailwayMapLayer
import de.hbch.traewelling.ui.composables.SwitchWithIconAndText
import de.hbch.traewelling.util.getJwtExpiration
import de.hbch.traewelling.util.refreshJwt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Settings(
    snackbarHostState: SnackbarHostState,
    loggedInUserViewModel: LoggedInUserViewModel? = null,
    emojiPackItemAdapter: EmojiPackItemAdapter? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DisplayProviderSettings(
            snackbarHostState = snackbarHostState
        )
        CheckInProviderSettings(
            snackbarHostState = snackbarHostState,
            loggedInUserViewModel = loggedInUserViewModel
        )
        HashtagSettings(
            snackbarHostState = snackbarHostState
        )
        TagsValueSettings()
        MapViewSettings()
        EmojiSettings(
            emojiPackItemAdapter = emojiPackItemAdapter
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun DisplayProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = context as ViewModelStoreOwner
    )
    val displayTagsInCheckInCard by settingsViewModel.displayTagsInCard.observeAsState(true)
    val displayJourneyNumber by settingsViewModel.displayJourneyNumber.observeAsState(true)
    val displayDivergentStop by settingsViewModel.displayDivergentStop.observeAsState(true)
    val useSystemFont by settingsViewModel.useSystemFont.observeAsState(false)

    SettingsCard(
        title = R.string.settings_display,
        description = R.string.settings_display_description,
        modifier = modifier,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SwitchWithIconAndText(
                checked = displayTagsInCheckInCard,
                onCheckedChange = {
                    settingsViewModel.updateDisplayTagsInCard(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_tag,
                stringId = R.string.settings_display_tags_in_card,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = displayJourneyNumber,
                onCheckedChange = {
                    settingsViewModel.updateDisplayJourneyNumber(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_train,
                stringId = R.string.settings_display_journey_number,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = displayDivergentStop,
                onCheckedChange = {
                    settingsViewModel.updateDisplayDivergentStop(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_navigation,
                stringId = R.string.settings_display_divergent_stop,
                modifier = Modifier.fillMaxWidth()
            )
            SwitchWithIconAndText(
                checked = useSystemFont,
                onCheckedChange = {
                    settingsViewModel.updateUseSystemFont(context, it)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.changes_saved)
                        )
                    }
                },
                drawableId = R.drawable.ic_font,
                stringId = R.string.use_system_font,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CheckInProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    loggedInUserViewModel: LoggedInUserViewModel? = null
) {
    SettingsCard(
        modifier = modifier,
        title = R.string.settings_check_in_providers,
        description = R.string.settings_check_in_providers_description,
        expandable = true
    ) {
        TraewellingProviderSettings(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth(),
            loggedInUserViewModel = loggedInUserViewModel
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp)
        )
        TravelynxProviderSettings(
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun TraewellingProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    loggedInUserViewModel: LoggedInUserViewModel? = null
) {
    if (loggedInUserViewModel != null) {
        val context = LocalContext.current
        val secureStorage = SecureStorage(context)
        var jwt by remember {
            mutableStateOf(
                secureStorage.getObject(
                    SharedValues.SS_JWT,
                    String::class.java
                ) ?: ""
            )
        }
        var defaultCheckIn by remember {
            mutableStateOf(
                secureStorage.getObject(
                    SharedValues.SS_TRWL_AUTO_LOGIN,
                    Boolean::class.java
                ) ?: true
            )
        }
        val username by loggedInUserViewModel.username.observeAsState("")
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trwl),
                    contentDescription = null
                )
                Text(
                    text = "Träwelling",
                    style = LocalFont.current.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.signed_in_as, username)
            )
            Text(
                text = stringResource(id = R.string.jwt_expiration, getJwtExpiration(jwt = jwt))
            )
            SwitchWithIconAndText(
                checked = defaultCheckIn,
                onCheckedChange = {
                    defaultCheckIn = it
                    secureStorage.storeObject(SharedValues.SS_TRWL_AUTO_LOGIN, it)
                },
                drawableId = R.drawable.ic_check_in,
                stringId = R.string.auto_check_in
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonWithIconAndText(
                    stringId = R.string.renew_login,
                    drawableId = R.drawable.ic_refresh,
                    onClick = {
                        context.refreshJwt(onTokenReceived = {
                            jwt = it
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.renew_login_success))
                            }
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
                ButtonWithIconAndText(
                    modifier = Modifier.weight(1f),
                    stringId = R.string.logout,
                    drawableId = R.drawable.ic_logout,
                    onClick = {
                        loggedInUserViewModel.logoutWithRestart(context)
                    }
                )
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TravelynxProviderSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val secureStorage = SecureStorage(context)
    var token by remember {
        mutableStateOf(
            secureStorage.getObject(
                SharedValues.SS_TRAVELYNX_TOKEN,
                String::class.java
            ) ?: ""
        )
    }
    var defaultCheckIn by remember {
        mutableStateOf(
            secureStorage.getObject(
                SharedValues.SS_TRAVELYNX_AUTO_CHECKIN,
                Boolean::class.java
            ) ?: false
        )
    }
    val saveTokenAction: () -> Unit = {
        keyboardController?.hide()
        secureStorage.storeObject(SharedValues.SS_TRAVELYNX_TOKEN, token)
        SharedValues.TRAVELYNX_TOKEN = token
        coroutineScope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.changes_saved))
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_travelynx),
                contentDescription = null
            )
            Text(
                text = "travelynx",
                style = LocalFont.current.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                value = token,
                singleLine = true,
                onValueChange = {
                    token = it
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.travelynx_token)
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.travelynx_token)
                    )
                }
            )
            FilledIconButton(
                onClick = saveTokenAction
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_in),
                    contentDescription = null
                )
            }
        }
        SwitchWithIconAndText(
            checked = defaultCheckIn,
            onCheckedChange = {
                defaultCheckIn = it
                secureStorage.storeObject(SharedValues.SS_TRAVELYNX_AUTO_CHECKIN, it)
            },
            drawableId = R.drawable.ic_check_in,
            stringId = R.string.auto_check_in
        )
        Text(
            text = stringResource(id = R.string.travelynx_limited_functionality),
            style = LocalFont.current.labelSmall,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HashtagSettings(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hashtagText by remember { mutableStateOf("") }
    var secureStorage: SecureStorage?
    var saveHashtagAction: () -> Unit = { }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    if (!LocalView.current.isInEditMode) {
        secureStorage = SecureStorage(context)
        hashtagText = secureStorage.getObject(SharedValues.SS_HASHTAG, String::class.java) ?: ""
        saveHashtagAction = {
            keyboardController?.hide()
            secureStorage.storeObject(SharedValues.SS_HASHTAG, hashtagText)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.changes_saved))
            }
        }
    }

    SettingsCard(
        modifier = modifier,
        title = R.string.hashtag,
        description = R.string.default_hashtag_text,
        expandable = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                value = hashtagText,
                singleLine = true,
                onValueChange = {
                    hashtagText = it
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hashtag),
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.hashtag)
                    )
                }
            )
            FilledIconButton(
                onClick = saveHashtagAction
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_in),
                    contentDescription = stringResource(id = R.string.store_hashtag)
                )
            }
        }
    }
}

@Composable
private fun MapViewSettings(
    modifier: Modifier = Modifier
) {
    var secureStorage: SecureStorage? = null
    var selectedOrmLayer by remember { mutableStateOf(OpenRailwayMapLayer.STANDARD) }

    if (!LocalView.current.isInEditMode) {
        secureStorage = SecureStorage(LocalContext.current)
        val storedOrmLayer =
            secureStorage.getObject(SharedValues.SS_ORM_LAYER, OpenRailwayMapLayer::class.java)
        storedOrmLayer?.let {
            selectedOrmLayer = it
        }
    }

    SettingsCard(
        modifier = modifier,
        title = R.string.map_view,
        description = R.string.configure_map,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(id = R.string.openrailwaymap)
                )
                OpenRailwayMapLayer.entries.forEach { layer ->
                    val layerSelected: () -> Unit = {
                        selectedOrmLayer = layer
                        secureStorage?.storeObject(SharedValues.SS_ORM_LAYER, selectedOrmLayer)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = layerSelected),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOrmLayer == layer,
                            onClick = layerSelected
                        )
                        Column {
                            Text(
                                text = stringResource(id = layer.title),
                                style = LocalFont.current.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(id = layer.description),
                                style = LocalFont.current.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiSettings(
    modifier: Modifier = Modifier,
    emojiPackItemAdapter: EmojiPackItemAdapter? = null
) {
    SettingsCard(
        modifier = modifier,
        title = R.string.emoji,
        description = R.string.emoji_text,
        expandable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (emojiPackItemAdapter != null) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val recyclerView = RecyclerView(context, null)

                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = emojiPackItemAdapter
                        recyclerView.isNestedScrollingEnabled = false

                        recyclerView
                    }
                )
            }
        }
    }
}

@Composable
private fun TagsValueSettings(
    modifier: Modifier = Modifier
) {
    SettingsCard(
        modifier = modifier,
        title = R.string.tag_default_values,
        description = R.string.tag_default_values_text,
        expandable = true
    ) {
        Column {
            TagType.entries.filter { it.ssDefaultValKey != null }.forEach {
                TagValueSetting(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagValueSetting(
    tag: TagType,
    modifier: Modifier = Modifier
) {
    var listVisible by remember { mutableStateOf(false) }
    var defaultValues by remember { mutableStateOf<List<String>>(emptyList()) }
    val newDefaultValue = rememberTextFieldState("")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val secureStorage = remember { SecureStorage(context) }

    val updateAndSaveValues: (List<String>) -> Unit = { newValues ->
        defaultValues = newValues
        if (tag.ssDefaultValKey != null) {
            coroutineScope.launch(context = Dispatchers.IO) {
                secureStorage.storeObject(key = tag.ssDefaultValKey, objectToStore = newValues)
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(id = tag.icon), contentDescription = null)
            Text(stringResource(id = tag.title))
        }
        Button(
            onClick = { listVisible = true }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = stringResource(id = R.string.title_edit)
            )
        }
    }
    if (listVisible) {
        ContentDialog(
            onDismissRequest = { listVisible = false },
            modifier = Modifier.padding(8.dp, 48.dp)
        ) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val storedArray = secureStorage.getObject(
                        (tag.ssDefaultValKey!!),
                        Array<String>::class.java
                    )
                    defaultValues = storedArray?.toMutableList() ?: emptyList()
                }
            }
            Column(
                modifier = Modifier
                    .padding(20.dp, 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = tag.title), style = MaterialTheme.typography.titleMedium )
                }
                val valueIsDuplicate = defaultValues.contains(newDefaultValue.text.toString())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val addNewValue: () -> Unit = {
                        val textToAdd = newDefaultValue.text.toString()
                        if (textToAdd.isNotBlank() && !valueIsDuplicate) {
                            val newList = listOf(textToAdd) + defaultValues
                            updateAndSaveValues(newList)
                            newDefaultValue.edit { replace(0, length, "") }
                        }
                    }
                    OutlinedTextField(
                        state = newDefaultValue,
                        modifier = Modifier.weight(1f),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onKeyboardAction = { addNewValue() },
                        isError = valueIsDuplicate
                    )
                    Button(onClick = addNewValue, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = stringResource(id = R.string.add)
                        )
                    }
                }
                AnimatedVisibility(visible = valueIsDuplicate, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(id = R.string.value_contained_in_list),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = defaultValues, key = { item -> item }) { defaultValue ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = defaultValue, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = {
                                val newList = defaultValues.filter { it != defaultValue }
                                updateAndSaveValues(newList)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = stringResource(id = R.string.delete)
                                )
                            }
                        }
                    }
                }
                Button(onClick = { listVisible = false }) {
                    Text(
                        text = stringResource(id = R.string.ok),
                        modifier = Modifier.padding(16.dp, 0.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
    @StringRes title: Int,
    @StringRes description: Int,
    content: @Composable (ColumnScope.() -> Unit)
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier,
        onClick = {
            if (expandable)
                expanded = !expanded
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = title),
                        style = LocalFont.current.headlineSmall
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(id = description),
                        style = LocalFont.current.labelSmall
                    )
                }
                if (expandable) {
                    IconToggleButton(
                        checked = expanded,
                        onCheckedChange = { expanded = !expanded }
                    ) {
                        val icon = if (expanded)
                            R.drawable.ic_expand_less
                        else
                            R.drawable.ic_expand_more

                        Icon(
                            painterResource(id = icon),
                            contentDescription = null
                        )
                    }
                }
            }
            AnimatedVisibility((expandable && expanded) || !expandable) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsCardPreview() {
    MainTheme {
        val title = R.string.hashtag
        val description = R.string.default_hashtag_text
        val content: @Composable ColumnScope.() -> Unit = {
            OutlinedTextField(
                value = "Test",
                onValueChange = { },
                label = {
                    Text(
                        text = stringResource(id = title)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                expandable = true,
                title = title,
                description = description,
                content = content
            )

            SettingsCard(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                expandable = false,
                title = title,
                description = description,
                content = content
            )
        }
    }
}
