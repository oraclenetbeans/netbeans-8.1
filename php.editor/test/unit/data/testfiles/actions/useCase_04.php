<?php

namespace Foo;

use Baz\Bat;

/**
 * Homepage presenter.
 *
 * @author     John Doe
 * @package    MyApplication
 */
class HomepagePresenter extends BasePresenter {

    public function renderDefault() {
        $this->template->anyVariable = 'any value';
        \Nette\Utils\Strings::capitalize();
        //Arrays::get($arr, $key);
        Html::el();
        new Request();
    }

    function functionName($param) {
        \Nette\Utils\Strings::capitalize();
    }

}

?>