<?php foreach ($news_list as $news) { ?>
    <div class="news_div">
        <span class="news_title"><?php echo $news->getTitle() ?></span>
        <span class="news_date_created"><?php echo $news->getDateCreated() ?></span>
        <br><br>
        <span class="new perex"></span>
    </div>
<?php } //end of foreach ?>


<div>
    <div>
        <?php if ($subtitle): ?>
            <div class="pageitem subtitle">
                <?php echo $subtitle; ?>
            </div>
            <div class="pageitem subtitle">
                <?php if ($subtitle): ?>
                    <div class="pageitem subtitle">
                        <?php echo $subtitle; ?>
                    </div>
                    <?php
            echo $ahoj;
            $ahoj = "nazdar";
                    ?>
                <?php endif; ?>
            </div>
        <?php endif; ?>
    </div>
</div>

<?php if ($subtitle): ?>
    <div class="pageitem subtitle">
        <?php echo $subtitle; ?>
    </div>
<?php endif; ?>