package com.group_7.mhd.cooker.Remote;

import com.group_7.mhd.cooker.Model.DataMessage;
import com.group_7.mhd.cooker.Model.MyResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                "Content-Type:application/json",
                "Authorization:key=AAAAZIHvH9g:APA91bE5pdfbZ8NXxEJEgLqoW0Sqg0SMeAtA6OpB0Xtssdhaefc2uArvIHx9PwSxJN0Am4MZqPYepyO2V-boSdzO_dlPUc5cnt2ytD3suw67JOVzPzENjeFLGkeOOClnI8YmfyOo4vEY"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body DataMessage body);
}
