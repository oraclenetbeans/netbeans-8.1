<?php foreach ($news_list as $news) { ?>
<div class="news_div">
<span class="news_title"><?php echo $news->getTitle() ?></span>
<span class="news_date_created"><?php echo $news->getDateCreated() ?></span>
<br><br>
<span class="new perex"></span>
</div>
<?php } //end of foreach ?>