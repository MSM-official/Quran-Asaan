package com.shoaib.qurani

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Total number of words (cards) in the book. */
const val WORD_COUNT = 451

/** Marks the Urdu title page (drawn by the app, not an image). */
const val TITLE = "title"

/** Index of word 1 in [SCREENS]. Word n is at index n + 1. */
const val FIRST_WORD = 2

/**
 * Every screen of the app, in order:
 * index 0            -> Urdu title page (فہمِ قرآن)
 * index 1            -> book cover
 * index 2..452       -> one word per screen (word n at index n + 1)
 * index 453, 454     -> author's note and summary pages
 */
val SCREENS: List<String> =
    listOf(TITLE, "pages/cover.webp") +
        (1..WORD_COUNT).map { "cards/w%03d.webp".format(it) } +
        listOf("pages/note.webp", "pages/summary.webp")

/** Nastaliq Urdu font (bundled at build time); falls back to the phone's font. */
var UrduFont: FontFamily = FontFamily.Default

val Cream = Color(0xFFFDFCBB)
val Brown = Color(0xFF8A3A12)
val DeepRed = Color(0xFFC62828)
val DeepGreen = Color(0xFF2E5E1E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UrduFont = try {
            FontFamily(Typeface.createFromAsset(assets, "fonts/nastaliq.ttf"))
        } catch (e: Exception) {
            FontFamily.Default
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF8A3A12),
                    background = Color.White,
                    surface = Color.White,
                ),
            ) {
                // Right-to-left like an Urdu book: page 1 is on the right,
                // the next word comes in from the left.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = Color.White) {
                        BookScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun BookScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("book", Context.MODE_PRIVATE) }
    // The app reopens on the page the user was reading when they closed it.
    // The first time ever, it opens on the title page.
    val lastPage = remember { prefs.getInt("last_page", 0).coerceIn(0, SCREENS.lastIndex) }
    val pagerState = rememberPagerState(
        initialPage = lastPage,
        pageCount = { SCREENS.size },
    )
    val scope = rememberCoroutineScope()
    var showGoTo by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }
    val activity = context as? Activity

    // Phone's Back button asks before closing the app.
    BackHandler { showClose = true }

    // Remember the last page that was open.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage > 0) {
            prefs.edit().putInt("last_page", pagerState.settledPage).apply()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFCEB))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // RTL: the close button sits on the right-hand side.
                TextButton(onClick = { showClose = true }) {
                    Text("✕  بند کریں", fontSize = 16.sp, color = DeepRed, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "فہمِ قرآن",
                    fontSize = 16.sp,
                    color = Brown,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFCEB))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // In RTL the first item is on the right side.
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0,
                ) { Text("پچھلا", fontSize = 16.sp) }

                Text(
                    text = labelFor(pagerState.currentPage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A3A12),
                    modifier = Modifier
                        .clickable { showGoTo = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )

                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = pagerState.currentPage < SCREENS.lastIndex,
                ) { Text("اگلا", fontSize = 16.sp) }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { page ->
            if (SCREENS[page] == TITLE) {
                TitlePage(
                    lastPage = lastPage,
                    onStart = { scope.launch { pagerState.animateScrollToPage(1) } },
                    onContinue = { scope.launch { pagerState.scrollToPage(lastPage) } },
                )
            } else {
                ZoomableAssetImage(
                    path = SCREENS[page],
                    isCurrent = pagerState.settledPage == page,
                )
            }
        }
    }

    if (showClose) {
        AlertDialog(
            onDismissRequest = { showClose = false },
            title = { Text("ایپ بند کریں؟") },
            text = { Text("کیا آپ ایپ بند کرنا چاہتے ہیں؟ اگلی بار آپ وہیں سے پڑھ سکیں گے جہاں چھوڑا تھا۔") },
            confirmButton = {
                TextButton(onClick = {
                    showClose = false
                    prefs.edit().putInt("last_page", pagerState.currentPage).commit()
                    activity?.finish()
                }) { Text("ہاں، بند کریں", color = DeepRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClose = false }) { Text("نہیں") }
            },
        )
    }

    if (showGoTo) {
        GoToDialog(
            onDismiss = { showGoTo = false },
            onGo = { number ->
                showGoTo = false
                scope.launch { pagerState.scrollToPage(number + FIRST_WORD - 1) }
            },
        )
    }
}

fun labelFor(page: Int): String = when {
    page == 0 -> "فہمِ قرآن"
    page == 1 -> "سرورق"
    page < FIRST_WORD + WORD_COUNT -> "نمبر ${page - FIRST_WORD + 1} / $WORD_COUNT"
    else -> "اختتامیہ"
}

/** Text style for Nastaliq: keeps tall letters from being cut off. */
fun urduStyle(size: Int, color: Color, lineHeight: Int = size * 2) = TextStyle(
    fontFamily = UrduFont,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = color,
    textAlign = TextAlign.Center,
    platformStyle = PlatformTextStyle(includeFontPadding = true),
)

/** The first screen: فہمِ قرآن — ڈاکٹر تنویر قاسم */
@Composable
fun TitlePage(lastPage: Int, onStart: () -> Unit, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(14.dp)
            .border(3.dp, Brown, RoundedCornerShape(18.dp))
            .padding(6.dp)
            .border(1.dp, Brown, RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                style = TextStyle(
                    fontSize = 20.sp,
                    color = DeepGreen,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(36.dp))
            Text(text = "فہمِ قرآن", style = urduStyle(56, DeepRed, 120))
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(Modifier.width(140.dp), thickness = 2.dp, color = Brown)
            Spacer(Modifier.height(8.dp))
            Text(text = "آسان قرآنی قطعے", style = urduStyle(24, Brown))
            Spacer(Modifier.height(40.dp))
            Text(text = "ڈاکٹر تنویر قاسم", style = urduStyle(30, Color(0xFF222222), 70))
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Brown),
            ) { Text("شروع کریں", fontSize = 18.sp) }
            if (lastPage > 1) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onContinue) {
                    Text("جہاں چھوڑا تھا: ${labelFor(lastPage)}", fontSize = 15.sp, color = Brown)
                }
            }
        }
    }
}

@Composable
fun GoToDialog(onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val number = text.trim().toIntOrNull()
    val valid = number != null && number in 1..WORD_COUNT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نمبر پر جائیں") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                label = { Text("1 سے $WORD_COUNT تک") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onGo(number!!) }, enabled = valid) { Text("جائیں") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("واپس") }
        },
    )
}

/** Loads an image from the assets folder in the background. */
@Composable
fun rememberAssetBitmap(path: String): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }
    }
    return bitmap
}

/**
 * Shows one page. Pinch with two fingers to zoom, drag to move when zoomed,
 * double-tap to zoom in / back out. With one finger and no zoom, the swipe
 * goes to the pager so you can turn the page.
 */
@Composable
fun ZoomableAssetImage(path: String, isCurrent: Boolean) {
    val bitmap = rememberAssetBitmap(path)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when the user moves away from this page.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val newScale = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxX = size.width * (newScale - 1) / 2
                            val maxY = size.height * (newScale - 1) / 2
                            val target = (center - tap) * (newScale - 1)
                            scale = newScale
                            offset = Offset(
                                target.x.coerceIn(-maxX, maxX),
                                target.y.coerceIn(-maxY, maxY),
                            )
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val maxX = size.width * (scale - 1) / 2
                            val maxY = size.height * (scale - 1) / 2
                            offset = if (scale <= 1f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            CircularProgressIndicator()
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}
