
## 環境構築

1. homebrewのインストール

````
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
````

2. vagrant と virtualboxインストール

````
brew cask install virtualbox
brew cask install virtualbox
````


3. isucon6q環境の構築

3.1 ワークフォルダ作成
````
mkdir ~/work
mv ~/work
````

3.2 構築用Vagrantファイルの取得

````
curl -O https://raw.githubusercontent.com/matsuu/vagrant-isucon/master/isucon6-qualifier-standalone/Vagrantfile
````


3.3 vm起動、プロビジョニング
````
vagrant up
````


3.4 Javaソース、プログラムの取得

````
git clone https://github.com/tkapp/isucon6-qualify.git
````


4. vm内の設定

4.1 vmにログイン
````
vagrant ssh
````

4.2 Javaの設定
````
sudo ln -s /vagrant/isucon6-qualify/webapp/java /home/isucon/webapp
sudo chmod 755 /home/isucon/webapp/java/*sh
sudo /home/isucon/webapp/java/deploy.sh
````

5. サービス起動
````
systemctl start isuda.java
systemctl start isutar.java
````

## WEB画面を見る


http://{{ vmのIP }}/

※vmのIPは、vmにログインして "ip a" と叩くとでます。


## ベンチマークの実行

vmにログインしてから、下記を実行します。

````
sudo su - isucon
cd isucon6q
./isucon6q-bench
````

## プログラムの修正

ソースの場所

シンボリックリンクを張ってあるので、Mac上で修正してもVM上で修正してもかまいません。

■Mac
~/work/isucon6-qualify/webapp/java/isuda/src/main/java/isucon6/web/Isuda.java
~/work/isucon6-qualify/webapp/java/isutar/src/main/java/isucon6/web/Isutar.java

■VM
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
