package com.raywenderlich.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*


class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val TAG = javaClass.simpleName
        val itunesService = ItunesService.instance
        val itunesRepo = ItunesRepo(itunesService)
        itunesRepo.searchByTerm("Android Developer") {
            Log.i(TAG, "Results = $it")
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
    }


    // search widget
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE)
                as SearchManager
        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(componentName)
        )
        if (podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcasts(term) { results ->
            hideProgressBar()
            toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    // set up view models
    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        val rssService = FeedService.instance
        podcastViewModel.podcastRepo = PodcastRepo(rssService)
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context,
            layoutManager.orientation
        )
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter
    }

    // called when the user taps on a podcast
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    //  called when the user taps on a podcast
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    // create details fragment
    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?
        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    // responds to changes in details fragment
    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    // displays the details Fragment:
    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT
        )
            .addToBackStack("DetailsFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    // helper method
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    // identifies details fragment
    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }
}