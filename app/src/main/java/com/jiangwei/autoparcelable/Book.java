package com.jiangwei.autoparcelable;

import com.jiangwei.annotation.Parcelable;
import com.jiangwei.autoparcelable.Person;

/**
 * author: jiangwei18 on 17/4/24 10:07 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class Book {
    @Parcelable
    double a;
    @Parcelable
    String b;
    @Parcelable
    float nums;
    @Parcelable
    int page;
    @Parcelable
    Person person;
}
