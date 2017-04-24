package com.jiangwei.autoparcelable;

import com.jiangwei.annotation.Parcelable;

/**
 * author: jiangw ei18 on 17/4/24 00:29 email: jiangwei18@baidu.com Hi: jwill金牛
 */

public class Person {
    @Parcelable
    int age;
    @Parcelable
    Book book;
}
