<?php

class Traited {
use FirstTrait;
}

class Traited2 {
use FirstTrait, Secondtrait;
}

class Traited3 {
use FirstTrait, Secondtrait{foo as bar;}
}

class Traited4 {
use FirstTrait, Secondtrait{foo as bar;FirstTrait::baz insteadof Secondtrait;}
}

?>