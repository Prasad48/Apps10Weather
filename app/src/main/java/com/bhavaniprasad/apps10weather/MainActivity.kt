package com.bhavaniprasad.apps10weather

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bhavaniprasad.apps10weather.viewmodel.WeatherRepository
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.Main
import source.open.akash.mvvmlogin.Model.current.WeatherReport
import source.open.akash.mvvmlogin.Model.nextdayforecast.ListData
import source.open.akash.mvvmlogin.Model.nextdayforecast.WeatherNextDaysReport
import java.lang.Runnable
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG: String? = MainActivity::class.java.simpleName
    var access_key: String = "1a97938424cf6f5d46eedd07a2c20add"
    var query: String = "Bangalore"

    //  Check Internet Continuosly
    var TYPE_WIFI = 1
    var TYPE_MOBILE = 2
    var TYPE_NOT_CONNECTED = 0
    var internetConnected = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val date_n = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

        var currentCity = query.split(",")


        if(checkConnectivity(applicationContext))
        {
            progress_bar.visibility= View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0..50) {
                    delay(5)
                    withContext(Dispatchers.Main) {
                        progress_bar.progress = i
                    }
                }
                runOnUiThread(Runnable {
                    getWeatherReport()
                    slideUp(lay_nextdays)
                    progress_bar.visibility= View.INVISIBLE
                    tv_current_location.text = currentCity[0]
                })

            }
        }else {
            shownointernet()
        }


        //declaring swipe refresh listener
        swipeRefresh.setOnRefreshListener {

            if(checkConnectivity(applicationContext))
            {
                //Fetch Current Weather and Next Day Forecasting repository data
                getWeatherReport()
                slideUp(lay_nextdays)
            }else
            {
                shownointernet()
            }
        }
    }

    private fun shownointernet() {
        if(checkConnectivity(applicationContext)){
            no_network2.visibility= View.INVISIBLE
        }
        else{
            no_network2.visibility= View.VISIBLE
        }
        no_network2.retry2.setOnClickListener{
            getWeatherReport()
        }
    }


    private fun getWeatherReport() {

        val weatherRepository = ViewModelProvider(this).get(WeatherRepository::class.java)

        GetCurrentWeather(weatherRepository)

        NextDayWeather(weatherRepository)


    }

    fun GetCurrentWeather(weatherRepository: WeatherRepository) {
        swipeRefresh.isRefreshing = true
        weatherRepository.getCurrentWeatherReport(access_key, query).observe(this, object : Observer<WeatherReport> {

            override fun onChanged(weatherReport: WeatherReport?) {


                if (weatherReport == null) {
                    swipeRefresh.isRefreshing = false
                    val builder = AlertDialog.Builder(this@MainActivity)
                    //set title for alert dialog
                    builder.setTitle(R.string.dialogTitle)
                    //set message for alert dialog
                    builder.setMessage(R.string.dialogMessage)
                    builder.setIcon(R.drawable.ic_error)

                    //performing positive action
                    builder.setPositiveButton("Retry") { dialogInterface, which ->
                        GetCurrentWeather(weatherRepository)
                    }

                    //performing negative action
                    builder.setNegativeButton("Exit") { dialogInterface, which ->
                        finish()
                    }
                    // Create the AlertDialog and set Properties
                    val alertDialog: AlertDialog = builder.create()
                    alertDialog.setCancelable(false)
                    alertDialog.show()

                } else {
                    val currentTempKelvin = weatherReport?.main?.temp
                    val currentTempCelsius = currentTempKelvin?.minus(273.15)

                    //set current temperature.
                    tv_current_temperature.text = currentTempCelsius?.toInt().toString()
                    degreec.text=resources.getString(R.string.degree_celsius_symbol)
                    if (weatherReport != null) {
                        val formatter = SimpleDateFormat("HH:mm:ss a ", Locale.US)
                        formatter.timeZone = TimeZone.getDefault()
                        swipeRefresh.isRefreshing = false
                    }

                }


            }


        })
    }

    fun NextDayWeather(weatherRepository: WeatherRepository) {

        swipeRefresh.isRefreshing = true
        weatherRepository.getForecastWeatherReport(access_key, query)
                .observe(this, object : Observer<WeatherNextDaysReport> {
                    override fun onChanged(weatherReport: WeatherNextDaysReport?) {
                        if (weatherReport == null) {
                            swipeRefresh.isRefreshing = false
                            val builder = AlertDialog.Builder(this@MainActivity)
                            //set title for alert dialog
                            builder.setTitle(R.string.dialogTitle)
                            //set message for alert dialog
                            builder.setMessage(R.string.dialogMessage)
                            builder.setIcon(R.drawable.ic_error)

                            //performing positive action
                            builder.setPositiveButton("Retry") { dialogInterface, which ->
                                NextDayWeather(weatherRepository)
                            }

                            //performing negative action
                            builder.setNegativeButton("Exit") { dialogInterface, which ->
                                finish()
                            }
                            // Create the AlertDialog and set Properties
                            val alertDialog: AlertDialog = builder.create()
                            alertDialog.setCancelable(false)
                            alertDialog.show()
                        } else {


                            val stringListDataHashMap = HashMap<String, ListData>()

                            if (weatherReport.list.isEmpty()) {

                                swipeRefresh.isRefreshing = false

                            } else {

                                //
                                for (y in 0 until weatherReport.list.size) {

                                    stringListDataHashMap[weatherReport.list.get(y).getDtTxt()!!] =
                                            weatherReport.list.get(y)
                                }


                                val listData = ArrayList<ListData>()

                                for (value in stringListDataHashMap.values) {
                                    System.out.println("Ascending before Value = " + value.getDtTxt())
                                    listData.add(value)

                                }

                                // Sort date in assending order
                                Collections.sort(listData, object : Comparator<ListData> {
                                    @SuppressLint("SimpleDateFormat")
                                    var f: DateFormat = SimpleDateFormat("yyyy-MM-dd")

                                    override fun compare(lhs: ListData, rhs: ListData): Int {
                                        try {
                                            return f.parse(lhs.getDtTxt()).compareTo(f.parse(rhs.getDtTxt()))
                                        } catch (e: ParseException) {
                                            e.printStackTrace()
                                            throw IllegalArgumentException(e)
                                        }

                                    }
                                })
                                rvNextDays.adapter = WeatherNextAdapter(applicationContext, listData)
                                rvNextDays.layoutManager = LinearLayoutManager(this@MainActivity)
                                swipeRefresh.isRefreshing = false
                            }
                        }
                    }

                })
    }


    class WeatherNextAdapter(var context: Context, var listDataList: List<ListData>) :
            RecyclerView.Adapter<WeatherNextAdapter.WeatherView>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WeatherView(
                LayoutInflater.from(parent.context).inflate(R.layout.item_next_forecast, parent, false)
        )


        override fun onBindViewHolder(holder: WeatherView, position: Int) {
            val listData = listDataList[position]
            if (position != 0) {
                if (position == 1) {
                    holder.tvDate.text = context.getString(R.string.tomorrow)
                } else {
                    holder.tvDate.setText(DateConvert(listData.getDtTxt()!!))
                }
                holder.tvDayName.setText(DayConvert(listData.getDtTxt()!!))
                holder.tvTemp.text =
                        String.format(
                                "%d%s",
                                (listData.main?.temp?.minus(273.15))?.toInt(),
                                Html.fromHtml(" \u2103")
                        )
            } else {
                holder.itemView.visibility = View.GONE
            }
        }


        override fun getItemCount(): Int {
            return listDataList.size
        }

        class WeatherView(itemView: View) : RecyclerView.ViewHolder(itemView) {


            val tvDate: TextView
            val tvDayName: TextView
            val tvTemp: TextView
            init {
                tvDate = itemView.findViewById(R.id.tvDate)
                tvDayName = itemView.findViewById(R.id.tvDayName)
                tvTemp = itemView.findViewById(R.id.tvTemp)
            }
        }

        //convert date format in dd-MM
        @SuppressLint("SimpleDateFormat")
        fun DateConvert(date: String): String {
            var dateString = date
            try {

                val sdf2 = SimpleDateFormat("dd-MM")
                val sdf = SimpleDateFormat("yyyy-MM-dd")
                dateString = sdf2.format(sdf.parse(dateString))

            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return dateString
        }

        //convert date format in day name
        @SuppressLint("SimpleDateFormat")
        fun DayConvert(dateString: String): String? {

            val sdf = SimpleDateFormat("yyyy-MM-dd")


            val sdf_ = SimpleDateFormat("EEEE")

            var dayName: String? = null
            try {
                dayName = sdf_.format(sdf.parse(dateString))
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return dayName
        }
    }

    // slide the view from below itself to the current position
    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
                0f, // fromXDelta
                0f, // toXDelta
                view.height.toFloat(), // fromYDelta
                0f
        )                // toYDelta
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }


    /**
     * Method to register runtime broadcast receiver to show snackbar alert for internet connection..
     */
    private fun registerInternetCheckReceiver() {
        val internetFilter = IntentFilter()
        internetFilter.addAction("android.net.wifi.STATE_CHANGE")
        internetFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(broadcastReceiver, internetFilter)
    }

    /**
     * Runtime Broadcast receiver inner class to capture internet connectivity events
     */
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = getConnectivityStatusString(context)
            setSnackbarMessage(status, false)
        }
    }

    fun getConnectivityStatus(context: Context): Int {
        val cm = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        if (null != activeNetwork) {
            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI

            if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE
        }
        return TYPE_NOT_CONNECTED
    }

    fun getConnectivityStatusString(context: Context): String? {
        val conn = getConnectivityStatus(context)
        var status: String? = null
        if (conn == TYPE_WIFI) {
            status = "Wifi enabled"
        } else if (conn == TYPE_MOBILE) {
            status = "Mobile data enabled"
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet"
        }
        return status
    }

    private fun setSnackbarMessage(status: String?, showBar: Boolean) {
        var internetStatus = ""

        try {
            if(checkConnectivity(applicationContext)){
                no_network2.visibility= View.INVISIBLE
            }
            else{
                no_network2.visibility= View.VISIBLE
            }
        } catch (e: Exception) {
            print(e)
        }

    }

    override fun onPause() {

        super.onPause()
        unregisterReceiver(broadcastReceiver)

    }

    override fun onResume() {
        super.onResume()
        registerInternetCheckReceiver()
    }
    //Check internet connectivity
    fun checkConnectivity(context: Context): Boolean {

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

        if (activeNetwork?.isConnected != null) {
            return activeNetwork.isConnected
        } else {
            return false
        }
    }
}
