package com.haero_kim.rxjava_practice

import android.app.SearchManager
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import com.haero_kim.rxjava_practice.utils.Constants.TAG
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.kotlin.subscribeBy

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private lateinit var myTextView: TextView

    // 서치뷰
    private lateinit var mySearchView: SearchView

    // 서치뷰 에딧 텍스트
    private lateinit var mySearchViewEditText: EditText

    // Disposable 을 모두 한번에 관리하는 CompositeDisposable
    // 옵저버블 통합 제거를 위해 사용 (메모리 릭 방지하기 위해 onDestroy() 에서 clear() 필요)
    private var myCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myTextView = findViewById(R.id.keyword_text)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "PhotoCollectionActivity - onCreateOptionsMenu() called")

        val inflater = menuInflater
        inflater.inflate(R.menu.top_app_bar_menu, menu)

        this.mySearchView = menu?.findItem(R.id.search_menu_item)?.actionView as SearchView

        this.mySearchView.apply {
            this.queryHint = "검색어를 입력해주세요"

            this.setOnQueryTextListener(this@MainActivity)

            // 서치뷰에서 에딧텍스트를 가져온다.
            mySearchViewEditText = this.findViewById(androidx.appcompat.R.id.search_src_text)

            // RxBinding 을 활용하여 EditText Observable 생성
            val editTextChangeObservable = mySearchViewEditText.textChanges()

            val searchEditTextSubscription: Disposable =
                    // 생성한 Observable 에 Operators 추가
                    editTextChangeObservable
                            // 마지막 글자 입력 0.8 초 후에 onNext 이벤트로 데이터 흘려보내기
                            .debounce(800, TimeUnit.MILLISECONDS)
                            // Scheduler 를 통해 IO 쓰레드에서 돌리겠다는 뜻
                            // 네트워크 (API) 요청, 파일 입출력, DB 쿼리 등
                            .subscribeOn(Schedulers.io())
                            // 구독을 통해 이벤트 응답 받기
                            .subscribeBy(
                                    onNext = {
                                        Log.d("Rx", "onNext : $it")
                                        // API 호출, DB 쿼리 등을 추상적으로 나타내는
                                        // TextView 텍스트 변경으로 처리함
                                        if (it.isNotEmpty()) {
                                            // UI 변경은 UI Thread 에서만
                                            runOnUiThread {
                                                myTextView.text = it.toString()
                                            }
                                        }
                                    },
                                    onComplete = {
                                        Log.d("Rx", "onComplete")
                                    },
                                    onError = {
                                        Log.d("Rx", "onError : $it")
                                    }
                            )
            // compositeDisposable 에 추가
            myCompositeDisposable.add(searchEditTextSubscription)
        }

        this.mySearchViewEditText.apply {
            this.setTextColor(Color.WHITE)
            this.setHintTextColor(Color.WHITE)
        }

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }

    override fun onDestroy() {
        // Observable 모두 삭제하여 메모리 관리
        this.myCompositeDisposable.clear()
        super.onDestroy()
    }
}