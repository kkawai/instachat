package com.instachat.android.adapter;

/**
 * Created by kevin on 10/14/2016.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a generic way of backing an RecyclerView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type.
 * <p>
 * To use this class in your app, subclass it passing in all required parameters and implement the
 * populateViewHolder method.
 * <p>
 * <blockquote><pre>
 * {@code
 *     private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
 *         TextView messageText;
 *         TextView nameText;
 * <p>
 *         public ChatMessageViewHolder(View itemView) {
 *             super(itemView);
 *             nameText = (TextView)itemView.findViewById(android.R.id.text1);
 *             messageText = (TextView) itemView.findViewById(android.R.id.text2);
 *         }
 *     }
 * <p>
 *     FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder> adapter;
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 * <p>
 *     RecyclerView recycler = (RecyclerView) findViewById(R.id.messages_recycler);
 *     recycler.setHasFixedSize(true);
 *     recycler.setLayoutManager(new LinearLayoutManager(this));
 * <p>
 *     adapter = new FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder>(ChatMessage.class, android.R.layout.two_line_list_item, ChatMessageViewHolder.class, ref) {
 *         public void populateViewHolder(ChatMessageViewHolder chatMessageViewHolder, ChatMessage chatMessage, int position) {
 *             chatMessageViewHolder.nameText.setText(chatMessage.getName());
 *             chatMessageViewHolder.messageText.setText(chatMessage.getMessage());
 *         }
 *     };
 *     recycler.setAdapter(mAdapter);
 * }
 * </pre></blockquote>
 *
 * @param <T>  The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The ViewHolder class that contains the Views in the layout that is shown for each object.
 */
public abstract class BaseMessagesAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private Class<T> mModelClass;
    private int mModelLayout;
    private Class<VH> mViewHolderClass;
    private List<T> mSnapshots;
    private ChildEventListener mChildEventListener;
    private Query mQuery;

    /**
     * @param modelClass      Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                        instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                        combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>
     */
    public BaseMessagesAdapter(final Class<T> modelClass, int modelLayout, Class<VH> viewHolderClass, Query ref) {
        mModelClass = modelClass;
        mModelLayout = modelLayout;
        mViewHolderClass = viewHolderClass;
        int maxMessageHistory = (int) FirebaseRemoteConfig.getInstance().getLong(Constants.KEY_MAX_MESSAGE_HISTORY);
        mSnapshots = new ArrayList<>(maxMessageHistory);
        mQuery = ref;

        mChildEventListener = ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                synchronized (BaseMessagesAdapter.this) {
                    T t = parseSnapshot(dataSnapshot);
                    if (isNewItemAllowed(t)) {
                        mSnapshots.add(t);
                        notifyItemInserted(mSnapshots.size() - 1);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                T t = parseSnapshot(dataSnapshot);
                synchronized (BaseMessagesAdapter.this) {
                    int index = mSnapshots.indexOf(t);
                    if (index != -1) {
                        checkItemBeforeChanging(index, t);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                T t = parseSnapshot(dataSnapshot);
                synchronized (BaseMessagesAdapter.this) {
                    int index = mSnapshots.indexOf(t);
                    if (index != -1) {
                        mSnapshots.remove(index);
                        notifyItemRemoved(index);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    protected final void replaceItem(int index, T item) {
        mSnapshots.remove(index);
        mSnapshots.add(index, item);
    }

    /**
     * Make sure to call replaceItem() within the implementation
     * of this method after determining what changed.  Then
     * call notifyItemChanged() passing in the optional payload
     * if it's only a partial change!
     *
     * @param index
     */
    protected abstract void checkItemBeforeChanging(int index, T item);

    /**
     * @param modelClass      Firebase will marshall the data at a location into an instance of a class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list. You will be responsible for populating an
     *                        instance of the corresponding view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location, using some
     *                        combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()</code>
     */
    public BaseMessagesAdapter(Class<T> modelClass, int modelLayout, Class<VH> viewHolderClass, DatabaseReference ref) {
        this(modelClass, modelLayout, viewHolderClass, (Query) ref);
    }

    public void cleanup() {
        //mSnapshots.cleanup();
        mQuery.removeEventListener(mChildEventListener);
    }

    @Override
    public int getItemCount() {
        synchronized (this) {
            return mSnapshots.size();
        }
    }

    public T getItem(int position) {
        synchronized (this) {
            return mSnapshots.get(position);
        }
    }

    /**
     * This method parses the DataSnapshot into the requested type. You can override it in subclasses
     * to do custom parsing.
     *
     * @param snapshot the DataSnapshot to extract the model from
     * @return the model extracted from the DataSnapshot
     */
    protected T parseSnapshot(DataSnapshot snapshot) {
        return snapshot.getValue(mModelClass);
    }

//    public DatabaseReference getRef(int position) {
//        return mSnapshots.getItem(position).getRef();
//    }

    @Override
    public long getItemId(int position) {
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        //return mSnapshots.getItem(position).getKey().hashCode();
        synchronized (this) {
            return mSnapshots.get(position).hashCode();
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(mModelLayout, parent, false);
        try {
            Constructor<VH> constructor = mViewHolderClass.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position, List<Object> payloads) {
        T model = getItem(position);
        populateViewHolder(viewHolder, model, position, payloads);
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        T model = getItem(position);
        populateViewHolder(viewHolder, model, position, null);
    }

    /**
     * Each time the data at the given Firebase location changes, this method will be called for each item that needs
     * to be displayed. The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     *
     * @param viewHolder The view to populate
     * @param model      The object containing the data used to populate the view
     * @param position   The position in the list of the view being populated
     */
    abstract protected void populateViewHolder(VH viewHolder, T model, int position, List<Object> payloads);

    abstract protected boolean isNewItemAllowed(T model);

    protected List<T> getData() {
        return mSnapshots;
    }

    protected void removeItemRemotely(String ref, OnCompleteListener onCompleteListener) {
        FirebaseDatabase.getInstance().getReference(ref).removeValue().addOnCompleteListener(onCompleteListener);
    }

    protected void removeItemLocally(int position) {
        synchronized (this) {
            mSnapshots.remove(position);
            notifyItemRemoved(position);
        }
    }
}
