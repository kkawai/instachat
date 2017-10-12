package com.instachat.android.app.activity.group;

import com.instachat.android.app.ui.base.BaseViewModel;
import com.instachat.android.data.DataManager;
import com.instachat.android.util.rx.SchedulerProvider;

public class GroupChatViewModel extends BaseViewModel<GroupChatNavigator> {

    //public ObservableArrayList<ItemViewModel> list = new ObservableArrayList<>();

    public GroupChatViewModel(DataManager dataManager, SchedulerProvider schedulerProvider) {
        super(dataManager, schedulerProvider);
    }

    /*public void fetchHomeData(List<Item> cache) {

        if (cache != null) {
            getNavigator().updateItems(cache);
            return;
        }
        setIsLoading(true);
        getCompositeDisposable()
                .add(getDataManager()
                        .getHomeData()
                        .subscribeOn(getSchedulerProvider().io())
                        .observeOn(getSchedulerProvider().ui())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                setIsLoading(false);
                            }
                        })
                        .subscribe(new Consumer<HomeResponse>() {
                            @Override
                            public void accept(@NonNull HomeResponse homeResponse)
                                    throws Exception {
                                getNavigator().updateItems(homeResponse.getItems());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable)
                                    throws Exception {
                                getNavigator().handleError(throwable);
                            }
                        }));
    }*/

    /*public void populateViewModel(List<Item> items) {

        if (Build.VERSION.SDK_INT >= 24) {
            list.addAll(items.stream().map(item -> new ItemViewModel(item)).collect(Collectors.toList()));
        } else {
            for (Item item : items) {
                list.add(new ItemViewModel(item));
            }
        }
    }*/

    /**
     * For caching purposes.  Returns a new array list of Item backed by
     * the original list of ItemViewModel.  Could use java8 for this later.
     * Useful for saving the array list into saved instance state when
     * screen is rotated.
     *
     * @return ArrayList<Item>
     */
    /*public ArrayList<Item> convert() {

        if (Build.VERSION.SDK_INT >= 24) {
            return new ArrayList<>(list.stream().map(ItemViewModel::getItem).collect(Collectors.toList()));
        } else {
            ArrayList<Item> arrayList = new ArrayList<>(list.size());
            int size = list.size();
            for (int i = 0; i < size; i++) {
                arrayList.add(list.get(i).getItem());
            }
            return arrayList;
        }
    }*/

}
