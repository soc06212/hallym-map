package xyz.ridsoft.hal.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.LayerDrawable
import android.os.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import xyz.ridsoft.hal.MainActivity
import xyz.ridsoft.hal.R
import xyz.ridsoft.hal.api.ApplicationPermissionManager
import xyz.ridsoft.hal.data.DataManager
import xyz.ridsoft.hal.data.GeoCoordinate
import xyz.ridsoft.hal.databinding.FragmentMapBinding
import xyz.ridsoft.hal.model.Facility
import xyz.ridsoft.hal.model.MapPoint
import xyz.ridsoft.hal.model.Place
import kotlin.collections.ArrayList

class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding

    private var markerData: MutableMap<String, MapPoint> = mutableMapOf()
    private var overlays: MutableMap<Facility.Companion.FacilityType, Overlay> = mutableMapOf()

    private var activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Handler(Looper.getMainLooper()).postDelayed({
                (activity as MainActivity).performCircularHideAnimation()
            }, 50)
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                it.data?.let { intent ->
                    if (intent.hasExtra(SearchActivity.INT_RESULT_ID)) {
                        val result = intent.getIntExtra(SearchActivity.INT_RESULT_ID, -1)
                        if (result < 0) {
                            return@registerForActivityResult
                        }

                        val point = (DataManager.facilitiesById + DataManager.placesById)[result]
                        point?.let { f ->
                            removeAllPin()

                            val overlay = convertToOverlay(arrayOf(f))
                            addOverlayPin(overlay)
                        }
                    }
                }
            }
        }

    public var onClickListener: ((View) -> Unit) = {
        // Haptic feedback
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 70))
        }

        // Activity transition animation
        (activity as MainActivity).performCircularRevealAnimation()
        // Start activity
        val intent = Intent(requireActivity(), SearchActivity::class.java)
        activityResultLauncher.launch(intent)

        // Transition animation
        activity?.overridePendingTransition(0, R.anim.anim_fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view)

        initDefaultData()
        initMapView()
        initChipView()

        binding.buttonMapCurrent.setOnClickListener {
            val permissionManager = ApplicationPermissionManager(requireContext())
            if (permissionManager.checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val myOverlay = MyLocationNewOverlay(binding.mapView)
                myOverlay.enableFollowLocation()
                myOverlay.enableMyLocation()
                myOverlay.setPersonIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_map_person
                    )
                )
                binding.mapView.overlays.add(myOverlay)
            } else {
                permissionManager.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

    }

    private fun initDefaultData() {
        val mapData = DataManager(requireContext())
        DataManager.places = mapData.getDefaultPlaceData() ?: arrayOf()
        DataManager.facilities = mapData.getDefaultFacilityData() ?: arrayOf()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMapView() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)

        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.isClickable = true
        binding.mapView.controller.setZoom(18.0)
        val startPoint =
            GeoPoint(GeoCoordinate.UNIVERSITY.latitude, GeoCoordinate.UNIVERSITY.longitude)
        binding.mapView.controller.setCenter(startPoint)
        binding.mapView.setOnClickListener {
            binding.mapView.overlays.removeIf { overlay ->
                !overlays.containsValue(overlay)
            }
        }

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                removeAllPin()
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean { return false }
        }
        val eventOverlay = MapEventsOverlay(receiver)
        binding.mapView.overlays.add(eventOverlay)

        val box = BoundingBox(37.8898, 127.7431, 37.8827, 127.7333)
        binding.mapView.setScrollableAreaLimitDouble(box)

    }

    @SuppressLint("ResourceType")
    private fun initChipView() {
        val types = Facility.Companion.FacilityType.getArray()

        for (type in types) {
            val chip = Chip(requireContext())

            chip.text = Facility.Companion.FacilityType.getString(requireContext(), type)
            chip.isCheckable = true
            chip.isCloseIconVisible = false
            chip.chipBackgroundColor =
                requireContext().resources.getColorStateList(R.drawable.background_tag, null)

            val drawable = AppCompatResources.getDrawable(
                requireContext(),
                Facility.Companion.FacilityType.getIconId(type)
            )
            val layerDrawable = LayerDrawable(arrayOf(drawable))
            layerDrawable.setLayerInset(0, 4, 4, 4, 4)
            chip.chipIcon = layerDrawable
            chip.iconStartPadding = 16f

            val facList = ArrayList<Facility>()
            for (f in DataManager.facilities!!) {
                if (f.type == type) {
                    facList.add(f)
                }
            }
            overlays[type] = convertToOverlay(facList.toTypedArray())

            chip.setOnClickListener {
                val c = it as Chip
                overlays[type]?.let { overlays ->
                    if (c.isChecked) {
                        addOverlayPin(overlays)
                    } else {
                        removeOverlayPin(overlays)
                    }
                }

            }

            binding.chipMapTag.addView(chip)
        }

    }

    private fun addOverlayPin(overlay: Overlay) {
        binding.mapView.overlays.add(overlay)
        binding.mapView.invalidate()
    }

    private fun removeOverlayPin(overlay: Overlay) {
        binding.mapView.overlays.remove(overlay)
        binding.mapView.invalidate()
    }

    private fun removeAllPin() {
        binding.mapView.overlays.removeIf { overlay ->
            overlay !is MapEventsOverlay
        }
        binding.chipMapTag.clearCheck()
    }

    private fun convertToOverlay(mapPoints: Array<MapPoint>): Overlay {
        val overlayList = ArrayList<OverlayItem>()

        for (i in mapPoints.indices) {
            val geoPoint = GeoPoint(mapPoints[i].latitude, mapPoints[i].longitude)

            val overlayItem = OverlayItem(mapPoints[i].name, "snippet", geoPoint)
            val markerDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)
            overlayItem.setMarker(markerDrawable)

            overlayList.add(overlayItem)

            markerData[overlayItem.title] = mapPoints[i]
        }

        return ItemizedIconOverlay(
            overlayList,
            object : OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    binding.mapView.controller.setZoom(19.5)
                    val startPoint =
                        GeoPoint(
                            item?.point?.latitude ?: GeoCoordinate.UNIVERSITY.latitude,
                            item?.point?.longitude ?: GeoCoordinate.UNIVERSITY.longitude
                        )
                    binding.mapView.controller.setCenter(startPoint)
                    markerData[item?.title]?.let { (activity as MainActivity).showBottomSheet(it) }
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    return true
                }

            },
            requireActivity().applicationContext
        )
    }

    override fun onDetach() {
        super.onDetach()
    }
}