# simpleforum

## 목차
[프로젝트 소개](#프로젝트-소개)  
[기존 프로젝트의 문제점](#기존-프로젝트의-문제점)  
[요구사항 분석](#요구사항-분석)  
[데이터베이스 모델](#데이터베이스-모델)  
[서버 구조](#서버-구조)  
[XSRF 문제 해결](#XSRF-문제-해결)  
[XSS 방어](#XSS-방어)  
[웹 캐시 사용](#웹-캐시-사용)  

## 프로젝트 소개
기존에 제작했던 간단한 게시판 프로젝트의 문제점을 개선하여 다시 제작한 프로젝트이다.   
(기존 프로젝트: https://github.com/yellowsunn/security-project)<br/>   

개선된 프로젝트는 유효성 검사, xss, xsrf 에 초점을 맞춰 제작되었다.  
<br>
테스트 사이트: https://yellowsunn.com

## 기존 프로젝트의 문제점
### 1. 서버에서 유효성 검사 부재
기존 프로젝트는 프론트엔드에서만 유효성 검사를 수행한다.   
따라서 브라우저의 개발자도구에서 값을 변조하거나, 직접 백엔드 서버로 api 요청을 하면 유효하지 않은 input이 들어올 수 있는 문제를 가지고 있다.

### 2. 적절하지 못한 HTTP 상태코드
클라이언트에서 잘못된 input을 주는 경우에도 서버에서 500에러 상태코드를 출력하는 등의 예외처리가 부족해 상황에 맞지않는 상태코드를 보여주는 문제가 있다.

### 3. XSS에 취약
게시판의 게시글은 html 태그를 사용할 수 있으므로 스크립트 코드가 삽입되면 게시글을 조회하는 순간 스크립트가 실행되는 문제점이 있으므로 XSS공격이 가능하다.   
기존 프로젝트는 클라이언트의 에디터가 적절하게 태그를 필터링해주나 서버에는 전혀 필터링을 하지 않는다.   
악의적인 사용자가 우회해서 태그를 삽입해 요청하면 속수무책으로 당하고 만다.

### 4. XSRF 문제점   
* CSRF 토큰
<img src="/readme_file/csrf_token.png" width="35%">

```java
protected void configure(HttpSecurity http) throws Exception {
	http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
}
```
기존 프로젝트는 XSRF를 방지하기 위해 다음과 같은 스프링 시큐리티의 CSRF 토큰 기능을 사용했었다.   
서버는 세션별로 UUID 값을 가진 토큰을 쿠키로 발급해주고, 클라이언트에서는 요청에 X-XSRF-TOKEN 헤더에 토큰으로 받은 UUID값을 전달해야만 서버가 요청을 받아들인다.
<br><br>

* 문제점
<img src="/readme_file/csrf_failed.png" width="90%">
<img src="/readme_file/csrf_success.png" width="90%">
문제점은 CSRF 토큰을 쿠키 방식으로 사용하는데 있다.<br>

해당 쿠키는 '**HttpOnly**' 속성이 없으므로 같은 도메인 내에서는 스크립트 코드에서 읽을 수 있다.   
따라서 위의 예시처럼 순진한 사용자가 _http://hacker.com_ 에 접근하면 쿠키의 값을 읽어오지 못해 요청이 무시되지만,  
같은 도메인인 _https://yellowsunn.com/posts/10_ 에 접근하면 쿠키의 값을 읽어 CSRF 공격에 성공하게 된다.

## 요구사항 분석
1. 회원은 아이디, 비밀번호, 가입 날짜 정보를 가지고 있다.
2. 회원으로 가입하려면 아이디, 비밀번호를 입력해야한다.
3. 가입한 회원에게는 기본으로 'USER' 등급이 부여된다.
4. 회원 등급은 'USER', 'ADMIN' 로 구성되어 있다.
5. 회원 아이디는 유일해야한다.
6. 게시글은 글 번호, 제목, 내용, 조회수, 작성 시간, 수정 시간 정보를 가지고 있다.
7. 게시글을 업로드 하려면 제목, 내용을 입력해야한다.
8. 회원은 게시글을 여러 개를  작성할 수 있고, 게시글 하나는 한 명의 회원만 작성할 수 있다.
9. 댓글은 내용, 작성 시간 정보를 가지고 있다.
10. 댓글은 여러개의 자식 댓글(답글)을 가질 수도 있다.
11. 회원은 댓글 여러 개를 작성할 수 있고, 댓글 하나는 한명의 회원만 작성할 수 있다.
12. 게시글에는 댓글 여러개가 달릴 수 있고, 댓글 하나는 한 게시글에만 속한다.
13. 파일은 파일이름 정보를 가지고 있다.
14. 게시글은 이미지 파일을 여러개 가질 수 있고, 이미지 파일은 한 게시글에만 속한다.
15. 게시글을 제거하면 게시글에 속한 댓글과 이미지도 모두 제거된다.

### 제한
* 회원의 아이디는 16자 이하의 영문 소문자와 숫자로만 구성된다.
* 회원의 비밀번호는 8~16 자리여야 한다.
* 게시글의 제목은 300자이하, 내용은 65535자 이하여야한다.
* 댓글의 내용은 1000자 이하여야 한다.
* 회원은 자신의 비밀번호만 수정할 수 있다.
* 회원은 자신을 회원 탈퇴 처리할 수 있고, 'ADMIN' 등급의 회원은 'USER' 등급의 회원을 탈퇴 처리 시킬 수 있다.
* 회원은 자신의 게시글을 수정, 삭제 할 수 있고, 'ADMIN' 등급의 회원은 'USER' 등급의 회원이 작성한 게시글을 삭제 할 수 있다. (수정할 수는 없다)

## 데이터베이스 모델

<img src="/readme_file/database_model.png" width="80%">

## 서버 구조
<img src="/readme_file/server_structure.jpg" width="80%">  
<br>

* 테스트 사이트 주소: https://yellowsunn.com
* 각 서버는 도커 컨테이너로 구성되어 있고 같은 네트워크에 연결되어 있다.
* Nginx의 reverse proxy 기능을 이용하여 클라이언트가 API나 이미지를 요청하는 경우에는 백엔드 서버를 호출하고,  
뷰페이지를 요청하는 경우에는 프론트엔드 서버를 호출한다.

### SSL
<img src="/readme_file/lets_encrypt.png" width="60%">

* HTTPS를 사용하기 위해서는 SSL인증서가 필요하므로 무료 SSL 발급 인증 기관인 Let's Encrypt에서 제공하는 Certbot Docker image를 이용하여 SSL 인증서를 발급했다.
* Let's Encrpyt에서 발급한 인증서의 유효기간은 90일이다. **따라서 90일마다 인증서를 재발급해야한다.**
	``` sh
	* 4 * * * /home/yellowsunn/simpleforum/docker/renew-ssl.sh
	```
	
	* **재발급 자동화 명령으로 해결**   
	Linux crontab에 위와 같은 명령어를 등록해 매일 04시마다 SSL인증서를 재발급을 요청하도록 했다.  
	_(Let's Encrpyt의 인증서는 만료 기간이 30일 이내일 때만 재발급이 가능하므로 그전까지는 재발급 요청이 무시된다.)_
	

## XSRF 문제 해결
### 문제 상황: Cross-Site에서 쿠키가 전달되는 문제
<img src="/readme_file/csrf.jpg" width="70%">

* 다음과 같이 악의적인 요청을 하는 Cross-Site에 들어가면 쿠키가 전달되어 csrf 공격을 당할 수 있다.

<br>

### ‣ 해결책: Same-Site=Lax or Strict
<img src="/readme_file/same-site_strict.jpg" width="70%">

* 단순하지만 효과적인 방법인 Same-Site 옵션을 쿠키에 추가해서 CSRF 공격을 예방할 수 있다.
* Cross-Site에서 쿠키가 전달되는 것을 방지할 수 있다.
--------------------------
### 문제 상황: Same-Site에서 쿠키가 전달되는 문제
<img src="/readme_file/same_site.jpg" width="70%">

* `Same-Site=Strict`옵션이 있더라도 다음과 같이 악의적인 사용자가 작성한 게시글에 접근하면 Same-Site에서는 쿠키가 전달된다.
<br>

### ‣ 해결책: Referer 헤더 체크
<img src="/readme_file/refer_check.jpg" width="70%">

```java
@DeleteMapping("api/users/{userId}")
public void deleteUser(@PathVariable Long userId) {
	refererFilter.check("/users");
	userService.deleteById(userId);
}
```

* 다음과 같이 서버에서는 `refererFilter` 라는 객체로 유효한 referer 헤더인지 사전에 체크를 한다.
	* 여기서는 referer 헤더가 https://yellowsunn.com/users 여야 유효한 요청으로 받아들인다.
* 일반적인 상황에서는 csrf 공격을 방지할 수 있으나, 예를 들어 패킷을 도중에 탈취해 referer 헤더를 변조해서 요청하는 등의 상황을 가정하면 완벽한 해결책은 아니다.
* Same-Site에서 CSRF를 방지하는 더 좋은 방법은 CSRF를 야기하는 XSS를 막는것이다.

## XSS 방어
### XSS Filter
직접 input으로 들어오는 html 태그를 필터링하는 방법도 있겠으나 네이버에서 오픈소스로 만든 `lucy-xss-servlet-filter`를 사용하면 안정적인 XSS 방어를 할 수 있다.

> https://github.com/naver/lucy-xss-servlet-filter

### 예시 
```html
<script>alert("xss")</script>
```
위와 같이 스크립트 코드를 서버에 전달하게 되면

```html
&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;
```
기호를 HTML 특수 코드로 변경해 XSS를 방어할 수 있다.

## 웹 캐시 사용
### 이미지를 조회하는데 웹 캐시를 사용
* 코드
```java
@GetMapping("/{fileName}")
public ResponseEntity<Resource> downloadImage(@PathVariable String fileName) throws IOException {
	Resource resource = new UrlResource("file:" + fileStore.getFullPath(fileName));
	if (!resource.exists()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

	return ResponseEntity.ok()
		.cacheControl(CacheControl.maxAge(Duration.ofDays(7L)))
		.lastModified(resource.lastModified())
		.contentType(fileStore.getImageContentType(resource.getFilename()))
		.body(resource);
}
```

* HTTP 응답 헤더 예시
```
Cache-Control: max-age=604800
Content-Type: image/jpeg
Last-Modified: Mon, 30 Aug 2021 15:32:45 GMT
```
