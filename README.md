RxJavaTest
===========

** Code Source
https://github.com/PacktPublishing/Reactive-Android-Programming

CH3 야후 주식 API Retrofit 연동  
------------
책에 나와있는 코드는 예전 야후 주식 API로 요청을 보내기 때문에 제대로 동작을 안함  
https://www.yahoofinanceapi.com/ 페이지를 참고하여 요청을 보내면됨(회원가입 필요)  
주식 API의 응답 Json 형태도 변경되었기 때문에 이에 맞춰 클래스를 바꿔줘야함  


CH5 StorIO 라이브러리를 이용한 Sqlite와 RxJava 연동
------------
StorIO가 RxJava1 만을 지원하기 때문에 asRxSingle() 뒤에 .subscribe()를 사용하지 못함
이를 위해 아래의 라이브러리 추가가 필요
implementation 'com.github.akarnokd:rxjava2-interop:0.13.7'