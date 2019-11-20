package kellinwood.zipsigner2.customkeys;

import java.util.List;

import android.view.LayoutInflater;
import kellinwood.zipsigner2.R;


import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KeystoreExpandableListAdapter  extends BaseExpandableListAdapter 
{
    ManageKeysActivity context;
    List<Keystore> keystores;
    
    public KeystoreExpandableListAdapter( ManageKeysActivity context, List<Keystore> keystores)
    {
        this.context = context;
        this.keystores = keystores;
    }

    public void dataChanged(List<Keystore> keystores) {
        this.keystores = keystores;
        notifyDataSetChanged();
    }
    
    public Alias getChild(int groupPosition, int childPosition) {
        return keystores.get(groupPosition).getAliases().get( childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        return keystores.get(groupPosition).getAliases().size();
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) 
    {
        
        final Alias childAlias = getChild(groupPosition, childPosition);

        LinearLayout linearLayout = null;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            linearLayout = (LinearLayout)inflater.inflate(R.layout.alias_item, null);

        } else linearLayout = (LinearLayout)convertView;


        CheckBox checkBox = (CheckBox)linearLayout.findViewById(R.id.AliasItemCheckBox);
        checkBox.setChecked( childAlias.isSelected());
        checkBox.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox)v;
                childAlias.setSelected(cb.isChecked());
                context.customKeysDataSource.updateAlias(childAlias);
            }
        });
        

        TextView textView = (TextView)linearLayout.findViewById(R.id.AliasItemTextView);

        String text = childAlias.getDisplayName();
        if (!childAlias.getName().equals(childAlias.getDisplayName())) {
            text = text + " (" + childAlias.getName() + ")";
        }
        text = text + "\n" + (childAlias.rememberPassword() ?
                context.getResources().getString(R.string.PasswordIsRemembered) : 
                    context.getResources().getString(R.string.PasswordIsNotRemembered));        
        textView.setText( text);


        return linearLayout;
    }

    public Keystore getGroup(int groupPosition) {
        return keystores.get( groupPosition);
    }

    public int getGroupCount() {
        return keystores.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) 
    {
        
        final Keystore keystore = getGroup( groupPosition);

        TextView textView = null;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            textView = (TextView)inflater.inflate(R.layout.keystore_group, null);

        } else textView = (TextView)convertView;

        textView.setText(keystore.getPath()
                + "\n" + (keystore.rememberPassword() ? 
                        context.getResources().getString(R.string.PasswordIsRemembered) : 
                        context.getResources().getString(R.string.PasswordIsNotRemembered)));



        return textView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

    public Keystore lookupKeystoreByPath( String keystorePath) {
        for (Keystore keystore : keystores) {
            if (keystore.getPath().equals(keystorePath)) return keystore;
        }
        return null;
    }
    
}
