<?php

copy($old,$new) or die("error");
copy($old,$new) xor die("error");
copy($old,$new) and die("error");

copy($old,$new) || die("error");
copy($old,$new) && die("error");

?>