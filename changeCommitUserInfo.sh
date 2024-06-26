git filter-branch -f --env-filter '
OLD_EMAIL="old@email.com"
CORRECT_NAME="lym"
CORRECT_EMAIL="lymdhr@qq.com"
if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_COMMITTER_NAME="$CORRECT_NAME"
    export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_AUTHOR_NAME="$CORRECT_NAME"
    export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags

# git push --force --tags git 'refs/heads/*'
# git push --force --tags origin 'refs/heads/*'

# git remote set-url github git@github.com:ChinaLym/shoulder-plugins.git
# 如果ssl有问题可以考虑 git config --global http.sslVerify"false"
# git push -u github main -f
# git push --delete origin <tagVersion>
