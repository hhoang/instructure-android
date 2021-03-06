package com.instructure.androidfoosball.activities

import android.app.AlertDialog
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.instructure.androidfoosball.App
import com.instructure.androidfoosball.R
import com.instructure.androidfoosball.adapters.UserAdapter
import com.instructure.androidfoosball.ktmodels.CutThroatGame
import com.instructure.androidfoosball.ktmodels.Game
import com.instructure.androidfoosball.ktmodels.User
import com.instructure.androidfoosball.receivers.GoalReceiver
import com.instructure.androidfoosball.utils.Const
import com.instructure.androidfoosball.utils.Prefs
import com.instructure.androidfoosball.utils.mCommentator
import com.instructure.androidfoosball.utils.shortToast
import com.instructure.androidfoosball.views.ConfirmPinDialog
import kotlinx.android.synthetic.tablet.activity_main.*
import org.jetbrains.anko.onClick
import java.util.*

import com.instructure.androidfoosball.ktmodels.Table
import org.jetbrains.anko.startActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val dateFormat = SimpleDateFormat("MMM d 'at' h:mm aa", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set volume stream to media to control commentator
        volumeControlStream = AudioManager.STREAM_MUSIC

        setupListeners()

        // Set table name
        tableNameView.text = Table.getSelectedTable().name

        @Suppress("DEPRECATION", "UsePropertyAccessSyntax")
        (motivosityWelcomeView.setText(Html.fromHtml(getString(R.string.motivosity_welcome))))

        GoalReceiver.register(this, goalReceiver)
    }

    override fun onResume() {
        super.onResume()

        // Show 'resume game' button if there are any ongoing games
        resumeGameButton.visibility = if (Table.hasOngoingGames()) View.VISIBLE else View.GONE
    }

    private val goalReceiver = GoalReceiver {
        mCommentator.announceBadTouch()
        Toast.makeText(this, "There is no game in progress", Toast.LENGTH_SHORT).show()
    }

    private fun setupListeners() {
        // Create new game
        newGameButton.onClick { startActivity<CreateGameActivity>() }

        // Cut-throat game
        cutthroatGameButton.onClick { startActivity<CreateCutThroatGameActivity>() }

        // Create new user
        addUserButton.onClick { startActivity<CreatePlayerActivity>() }

        // Show leaderboard
        leaderboardButton.onClick { startActivity<LeaderboardActivity>() }

        // Open settings
        clockView.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }

        // Switch tables
        tableNameView.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage("Assign this tablet to a different foosball table?")
                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                        Prefs.tableId = ""
                        finish()
                        startActivity<SyncActivity>()
                    }.setNegativeButton(android.R.string.no, null)
                    .show()
        }

        // Edit Player
        editPlayerButton.setOnClickListener {
            val users = App.realm.where(User::class.java).equalTo("guest", false).findAllSorted("name").toList()
            MaterialDialog.Builder(this@MainActivity)
                    .title(R.string.pick_a_user)
                    .adapter(UserAdapter(this@MainActivity, users)) { dialog, itemView, which, text ->
                        dialog.dismiss()
                        if (users[which].guest) {
                            shortToast(R.string.cannot_edit_guests)
                        } else {
                            ConfirmPinDialog(this@MainActivity, users[which]) { user ->
                                startActivity<EditUserActivity>(Const.USER_ID to users[which].id)
                            }.show()
                        }
                    }.show()
        }

        // Resume game
        resumeGameButton.onClick {
            val games = Table.getOngoingGames()
            MaterialDialog.Builder(this)
                    .items(games.map { game ->
                        when (game) {
                            is Game -> {
                                val dateString = dateFormat.format(Calendar.getInstance().apply { timeInMillis = game.startTime }.time)
                                "Game started $dateString with players ${(game.teamOne.users + game.teamTwo.users).joinToString { it.name }}"
                            }
                            is CutThroatGame -> {
                                val dateString = dateFormat.format(Calendar.getInstance().apply { timeInMillis = game.startTime }.time)
                                "Cut-Throat Game started $dateString with players ${game.players.joinToString { it.user.name }}"
                            }
                            else -> "Unknown game"
                        }

                    })
                    .itemsCallback { materialDialog, view, i, charSequence ->
                        val game = games[i]
                        when (game) {
                            is Game -> startActivity<GameActivity>(GameActivity.EXTRA_GAME_ID to game.id)
                            is CutThroatGame -> startActivity<CutThroatGameActivity>(CutThroatGameActivity.EXTRA_GAME_ID to game.id)
                        }
                    }
                    .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        GoalReceiver.unregister(this, goalReceiver)
    }

    override fun onBackPressed() {
        // Do nothing. This should act as the root activity.
    }
}
