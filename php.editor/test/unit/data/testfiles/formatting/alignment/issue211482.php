<?php

class A {

    public function __construct($a, $beta) {
        $this->param = $a;
        $this->does_not_indent_properly = $beta;

        $ashorVar = "short";
        $quiteLongVariable = "long";
    }

    public function fnc() {
        $this->param = $a;
        $this->does_not_indent_properly = $beta;
        $ashorVar = "short";
        $quiteLongVariable = "long";
    }

    public function fnc2() {
        $this->param = $a;

        $this->does_not_indent_properly = $beta;

        $ashorVar = "short";

        $quiteLongVariable = "long";
    }

}

?>