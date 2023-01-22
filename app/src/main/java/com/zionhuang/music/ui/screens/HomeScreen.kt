package com.zionhuang.music.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.innertube.models.*
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.ListItemHeight
import com.zionhuang.music.models.toMediaMetadata
import com.zionhuang.music.playback.queues.YouTubeQueue
import com.zionhuang.music.ui.component.LocalMenuState
import com.zionhuang.music.ui.component.SongListItem
import com.zionhuang.music.ui.component.YouTubeGridItem
import com.zionhuang.music.ui.menu.SongMenu
import com.zionhuang.music.ui.menu.YouTubeAlbumMenu
import com.zionhuang.music.ui.utils.SnapLayoutInfoProvider
import com.zionhuang.music.viewmodels.HomeViewModel
import com.zionhuang.music.viewmodels.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playWhenReady by playerConnection.playWhenReady.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val libraryAlbumIds by mainViewModel.libraryAlbumIds.collectAsState()

    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val newReleaseAlbums by viewModel.newReleaseAlbums.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val mostPlayedLazyGridState = rememberLazyGridState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val snapLayoutInfoProvider = remember(mostPlayedLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = mostPlayedLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .widthIn(min = 84.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                        .clickable {
                            navController.navigate("settings")
                        }
                        .padding(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (mostPlayedSongs.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.most_played_songs),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }

                item {
                    LazyHorizontalGrid(
                        state = mostPlayedLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 4)
                    ) {
                        items(
                            items = mostPlayedSongs,
                            key = { it.id }
                        ) { song ->
                            SongListItem(
                                song = song,
                                isPlaying = song.id == mediaMetadata?.id,
                                playWhenReady = playWhenReady,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    playerConnection = playerConnection,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(maxWidth * horizontalLazyGridItemWidthFactor)
                                    .combinedClickable {
                                        playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
                                    }
                            )
                        }
                    }
                }
            }

            if (newReleaseAlbums.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("new_release")
                            }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.new_release_albums),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.ic_navigate_next),
                            contentDescription = null
                        )
                    }
                }

                item {
                    LazyRow {
                        items(
                            items = newReleaseAlbums,
                            key = { it.id }
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                badges = {
                                    if (album.id in libraryAlbumIds) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_library_add_check),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                    if (album.explicit) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_explicit),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                },
                                isPlaying = mediaMetadata?.id == album.id,
                                playWhenReady = playWhenReady,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    album = album,
                                                    navController = navController,
                                                    playerConnection = playerConnection,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}
