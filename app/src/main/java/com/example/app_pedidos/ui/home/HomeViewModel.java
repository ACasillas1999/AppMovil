package com.example.app_pedidos.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private MutableLiveData<String> mOrderInfo; // Datos del pedido

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Home fragment");

        // Inicializa los datos del pedido
        mOrderInfo = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getOrderInfo() {
        return mOrderInfo;
    }

    // Actualiza los datos del pedido
    public void setOrderInfo(String orderInfo) {
        mOrderInfo.setValue(orderInfo);
    }
}

