## 環境構築

1. virtualboxインストール

下記のページの「Windows hosts」からインストーラーをダウンロードし、実行してください。
その後はウィザードに沿ってインストールします。

https://www.virtualbox.org/wiki/Downloads


2. vagrantのインストール

下記のページの「Windows hosts」からインストーラーをダウンロードし、実行してください。
その後はウィザードに沿ってインストールします。


https://www.vagrantup.com/downloads.html


3. ワークフォルダ作成

任意の場所にワークフォルダを作成します。

````cmd:
mkdir c:/develop/work
````


4. 構築用Vagrantファイルの取得


下記のファイルをワークフォルダに保存する。※拡張子が付かないように注意

https://raw.githubusercontent.com/tkapp/vagrant-isucon/master/isucon6-qualifier-standalone/Vagrantfile


5. vm起動、プロビジョニング

````
cd c:\develop\work
vagrant up
````

6. vmにログイン

teraterm、puttyなどのクライアントツールで、VMにログインします。

```
host名： localhost
port: 2022
user: ubuntu
password: C:/Users/{{ user }}/.vagrant.d/boxes/ubuntu-VAGRANTSLASH-xenial64/20170501.0.0/virtualbox/Vagrantfile の中に書いてあります。
```

7. Javaソースのダウンロードと初期設定

````
cd /vagrant
git clone https://github.com/tkapp/isucon6-qualify.git
sudo ln -s /vagrant/isucon6-qualify/webapp/java /home/isucon/webapp
sudo chmod 755 /home/isucon/webapp/java/*sh
sudo /home/isucon/webapp/java/deploy.sh
````


8. サービス起動

````
systemctl start isuda.java
systemctl start isutar.java
````


## WEB画面を見る

http://localhost:8881/



## ベンチマークの実行

vmにログインしてから、下記を実行します。

````
sudo su - isucon
cd isucon6q
./isucon6q-bench
````

## プログラムの修正

ソースの場所

/home/isucon/webapp/java/isuda/src/main/java/isucon6/web/Isuda.java
/home/isucon/webapp/java/isutar/src/main/java/isucon6/web/Isutar.java


## ビルド

vmにログインし、下記を実行してください。

````
sudo /home/isucon/webapp/java/build.sh
````

/home/isucon/webapp/java にある、isuda.jar、isutar.jarが更新されます。

その後、サービスを再起動してください。

````
systemctl restart isuda.java
systemctl restart isutar.java
````
