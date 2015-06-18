package trialbycombat.com.whengreen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import trialbycombat.com.whengreen.R;

public class LightExpandableListFragment extends Fragment {
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, Light> listDataChild;

    private ArrayList<Light> mLights;
    private Light mLight;
    private ImageView mDelButton;
    private ImageView  mGreenButton;
    private ExpandableListAdapter mAdapter;
    private TextView mNextGreenTextView,mLightNameTextView;
    private ImageButton mSaveButton;
    private CountDownTimer countDownTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fragment has menu items to add
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_light_expandable_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_new:
                Light newLight=new Light();
                newLight.setLightID(UUID.randomUUID());
                LightStatusHelper.get(getActivity()).UpdateLightDetails(getActivity(),newLight);
                RefreshAdapterDataSetChanged();
                return true;
//            case R.id.action_settings:
//
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_light_expandable_list, null);
        //set menu title
        getActivity().setTitle("Your lights...");

        expListView = (ExpandableListView) v.findViewById(R.id.light_list);
        expListView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        RefreshAdapterDataSetChanged();

        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                expListView.smoothScrollToPosition(groupPosition);

                //clear countdowns
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer=null;
                }

                //restart expanded group countdown
                mLight = (Light) listAdapter.getChild(groupPosition, 0);
                countDownTimer = new NextGreenCountDownTimer(getTimeRemainingToGreen(mLight.getLightActivityTimes()) * 1000, 1);
                countDownTimer.start();

                if(groupPosition != previousGroup) {
                    expListView.collapseGroup(previousGroup);
                }
                previousGroup = groupPosition;
            }
        });

        return v;
    }

    private void RefreshAdapterDataSetChanged() {
        mLights = LightStatusHelper.get(getActivity()).refreshLights();

        //refresh list data after delete
        prepareListData();
        listAdapter = new ExpandableListAdapter(getActivity(), listDataHeader, listDataChild);
        // setting list adapter
        expListView.setAdapter(listAdapter);
    }

    public class ExpandableListAdapter extends BaseExpandableListAdapter {
        private Context _context;
        private List<String> _listDataHeader; // header titles
        // child data in format of header title, child title
        private HashMap<String, Light> _listDataChild;

        public ExpandableListAdapter(Context context, List<String> listDataHeader, HashMap<String, Light> listChildData) {
            this._context = context;
            this._listDataHeader = listDataHeader;
            this._listDataChild = listChildData;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return this._listDataChild.get(this._listDataHeader.get(groupPosition));
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,boolean isLastChild, View convertView, ViewGroup parent) {

            mLight = (Light)getChild(groupPosition, childPosition);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.expandable_light_list_item, null);
            }

            mLightNameTextView = (TextView) convertView.findViewById(R.id.light_list_item_textview_light_name);

            mNextGreenTextView= (TextView) convertView.findViewById(R.id.light_list_item_textview_next_green);
            mNextGreenTextView.setText("00:00:00:00");

            mSaveButton = (ImageButton) convertView.findViewById(R.id.light_list_item_button_save);
            mSaveButton.setTag(mLight);
            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Light light=(Light)v.getTag();
                    light.setLightName(mLightNameTextView.getText().toString());
                    //if id empty, its a new light
                    if(light.getLightID()==null)
                        light.setLightID(UUID.randomUUID());

                    LightStatusHelper.get(getActivity()).UpdateLightDetails(getActivity(),light);
                    Toast.makeText(getActivity(), "Saved!", Toast.LENGTH_SHORT).show();

                    //refresh list data after delete
                    RefreshAdapterDataSetChanged();
                }
            });


            TextView txtLightName = (TextView) convertView.findViewById(R.id.light_list_item_textview_light_name);
            txtLightName.setText(mLight.getLightName());

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if(this._listDataChild.get(this._listDataHeader.get(groupPosition))!=null)
                return 1;
            return 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return this._listDataHeader.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return this._listDataHeader.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public void doPositiveClick() {
            // Do stuff here.

        }

        public void doNegativeClick() {
            // Do stuff here.

        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {


            String headerTitle = (String) getGroup(groupPosition);
            Light item=(Light)getChild(groupPosition, 0);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.expandable_light_list_group, null);
            }

            TextView titleTextView = (TextView) convertView.findViewById(R.id.light_list_item_nameTextView);

            if(headerTitle==null ||headerTitle.isEmpty())
                titleTextView.setText("?");
            else
                titleTextView.setText(headerTitle);

            if(isExpanded){
                convertView.setBackgroundResource(android.R.color.holo_orange_light);
            }else{
                convertView.setBackgroundResource(R.drawable.custom_list_item_shape);
            }

            mDelButton = (ImageView) convertView.findViewById(R.id.light_list_item_delete);
            mDelButton.setTag(item);
            mDelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConfirmDelete((Light)v.getTag());
                }
            });

            mGreenButton = (ImageView) convertView.findViewById(R.id.light_list_item_button_green);
            mGreenButton.setTag(item.getLightID());
            mGreenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String greenTime = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
                    LightStatusHelper.get(getActivity()).UpdateGreenStatus(getActivity(), (UUID) v.getTag(), greenTime);
                    Toast.makeText(getActivity(), greenTime + " Saved!", Toast.LENGTH_SHORT).show();
                }
            });


            TextView lblListHeader = (TextView) convertView.findViewById(R.id.light_list_item_nameTextView);
            lblListHeader.setTypeface(null, Typeface.BOLD);
            lblListHeader.setText(headerTitle);

            return convertView;
        }

        private void ConfirmDelete(Light light) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            final Light innerLight = light;
            dialog.setTitle("Sure?")
                    .setIcon(R.drawable.red_light)
                    .setMessage("Light and all saved data will be deleted. Are you sure?")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            dialoginterface.cancel();
                        }
                    })
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            LightStatusHelper.get(getActivity()).DeleteLightData(_context,innerLight.getLightID() );
                            Toast.makeText(getActivity(), innerLight.getLightName()+" Deleted!", Toast.LENGTH_SHORT).show();
                            RefreshAdapterDataSetChanged();
                        }
                    }).show();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    public class NextGreenCountDownTimer extends CountDownTimer {
        public NextGreenCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            int timeUntilNextGreen=getTimeRemainingToGreen(mLight.getLightActivityTimes());
            if(timeUntilNextGreen>0) {
                Toast.makeText(getActivity(), "Missed this one! Time until next green.", Toast.LENGTH_SHORT).show();
                this.cancel();
                countDownTimer = new NextGreenCountDownTimer(timeUntilNextGreen * 1000, 1);
                this.start();
            }
            else
                Toast.makeText(getActivity(), "Looks like you haven't been setting your greens! Set some from the big green button at the main menu.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mNextGreenTextView.setText("" + String.format("%02d:%02d:%02d:%02d", (millisUntilFinished/1000) / 3600,
                    ((millisUntilFinished/1000) % 3600) / 60, ((millisUntilFinished/1000) % 60), millisUntilFinished%1000));
        }
    }


    private ArrayList<String> RemoveEmptyAndNullElements(ArrayList<String> originalList ) {
        ArrayList filterList=new ArrayList();
        filterList.add(null);
        filterList.add("");

        originalList.removeAll(filterList);
        return originalList;
    }

    private int getTimeRemainingToGreen(ArrayList<String> times) {
        LocalTime nowMili = LocalTime.now();
        times = RemoveEmptyAndNullElements(times);

        if (times.size() > 0) {
            int difference = 0;
            int minDifference = 99999999;
            for (String time : times) {
                if (time != null && !time.isEmpty()) {
                    LocalTime timeMili = LocalTime.parse(time);

                    if (nowMili.isBefore(timeMili)) {
                        difference = Seconds.secondsBetween(nowMili, timeMili).getSeconds();
                    }
                    else
                    {
                        difference =(24*60*60) + Seconds.secondsBetween(nowMili,timeMili ).getSeconds();

                    }
                    if (difference < minDifference) {
                        minDifference = difference;
                    }
                }
            }
            return minDifference;
        } else{
            return 0;
        }
    }

    /*
         * Preparing the list data
         */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, Light>();

        for(Light light:mLights)
        {
            listDataHeader.add(light.getLightName());
            listDataChild.put(light.getLightName(), light);
        }
    }
}
