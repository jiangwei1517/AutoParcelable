# AutoParcelable
![MacDown logo](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1493036895782&di=db3de7ccf8eb4025717b4cf763557c5c&imgtype=0&src=http%3A%2F%2Fimage14-c.poco.cn%2Fmypoco%2Fmyphoto%2F20130117%2F14%2F66153885201301171441436381211846447_006.jpg%3F514x778_120)

## 解决问题
* 自动将对象转化成Parcelable对象

## 基本思想
* annotatoon
* javapoet

## 使用方法
***属性注解为@Parcelable***

	public class Person {
	    @Parcelable
	    int age;
	    @Parcelable
	    Book book;
	}
	
## 注意事项
* ***之前只针对自己项目，所以可转换的类型有限，仅支持String、int、double、float、Bundle、byte、自定义的bean类。有特殊需求，在ParcelableProcessor的judgeFieldType()方法中添加。***
* ***暂时不支持内部类。如需支持，在ParcelableProcessor中重新定义thatPackageName，thatClassName***
* ***代码仅为学习。有待完善。***